package com.huangyuanwei.aics.service;

import com.huangyuanwei.aics.common.AppConstants;
import com.huangyuanwei.aics.entity.KnowledgeBase;
import com.huangyuanwei.aics.enums.VectorStatus;
import com.huangyuanwei.aics.repository.KnowledgeBaseRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理服务
 * 负责文件上传 MinIO、Chunk 分割预览、Milvus 向量化
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final MinioClient minioClient;
    private final VectorStore vectorStore;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @Value("${minio.bucket.knowledge}")
    private String knowledgeBucket;

    @Value("${app.knowledge.chunk-size}")
    private int chunkSize;

    @Value("${app.knowledge.chunk-overlap}")
    private int chunkOverlap;

    /**
     * 上传文件并返回 Chunk 分割预览
     *
     * @return 包含 KnowledgeBase 记录和 chunk 预览列表
     */
    public Map<String, Object> uploadAndPreview(MultipartFile file, Long userId) throws Exception {
        // 1. 确保 Bucket 存在
        ensureBucketExists(knowledgeBucket);

        // 2. 上传原始文件到 MinIO
        String minioPath = generateMinioPath(file.getOriginalFilename());
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(knowledgeBucket)
                    .object(minioPath)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        }
        log.info("文件已上传到 MinIO: {}/{}", knowledgeBucket, minioPath);

        // 3. 创建 KnowledgeBase 记录
        KnowledgeBase kb = new KnowledgeBase();
        kb.setFileName(file.getOriginalFilename());
        kb.setFileSize(file.getSize());
        kb.setFileType(getFileExtension(file.getOriginalFilename()));
        kb.setMinioBucket(knowledgeBucket);
        kb.setMinioPath(minioPath);
        kb.setChunkSize(chunkSize);
        kb.setChunkOverlap(chunkOverlap);
        kb.setVectorStatus(VectorStatus.PENDING.getValue());
        kb.setUploadUserId(userId);
        kb = knowledgeBaseRepository.save(kb);

        // 4. 进行 Chunk 分割预览（不做向量化，仅供展示）
        List<Map<String, Object>> chunkPreviews = splitAndPreview(file);
        kb.setChunkCount(chunkPreviews.size());
        knowledgeBaseRepository.save(kb);

        // 5. 异步执行向量化
        vectorizeAsync(kb.getId(), file.getBytes(), kb.getFileName());

        return Map.of(
            "knowledgeBase", kb,
            "chunks", chunkPreviews
        );
    }

    /**
     * 生成 Chunk 分割预览（不保存向量，仅返回分割结果）
     */
    private List<Map<String, Object>> splitAndPreview(MultipartFile file) {
        List<Map<String, Object>> previews = new ArrayList<>();
        try {
            String text = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // 按自然段落简单分割作为预览
            String[] paragraphs = text.split("\n\n+");
            int index = 1;
            for (String para : paragraphs) {
                if (para.trim().length() > AppConstants.CHUNK_PREVIEW_MIN_CHARS) {
                    String preview = para.trim();
                    previews.add(Map.of(
                        "index", index++,
                        "content", preview.substring(0,
                                Math.min(AppConstants.CHUNK_PREVIEW_MAX_CHARS, preview.length())),
                        "charCount", preview.length()
                    ));
                }
                if (previews.size() >= AppConstants.CHUNK_PREVIEW_MAX_COUNT) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Chunk 预览生成失败: {}", e.getMessage());
        }
        return previews;
    }

    /**
     * 异步向量化并写入 Milvus
     */
    @Async
    public void vectorizeAsync(Long kbId, byte[] fileBytes, String fileName) {
        knowledgeBaseRepository.findById(kbId).ifPresent(kb -> {
            try {
                kb.setVectorStatus(VectorStatus.PROCESSING.getValue());
                knowledgeBaseRepository.save(kb);

                // 构建 Document 列表
                String text = new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
                Document doc = new Document(text,
                        Map.of("source", fileName, "kb_id", kbId.toString()));

                // Chunk 分割
                TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, chunkOverlap, 5, 10000, true);
                List<Document> chunks = splitter.apply(List.of(doc));

                // 写入 Milvus
                vectorStore.add(chunks);

                kb.setChunkCount(chunks.size());
                kb.setVectorStatus(VectorStatus.COMPLETED.getValue());
                kb.setVectorCollection(AppConstants.MILVUS_COLLECTION_NAME);
                knowledgeBaseRepository.save(kb);
                log.info("知识库向量化完成 kbId={}, chunks={}", kbId, chunks.size());
            } catch (Exception e) {
                log.error("知识库向量化失败 kbId={}", kbId, e);
                knowledgeBaseRepository.findById(kbId).ifPresent(k -> {
                    k.setVectorStatus(VectorStatus.FAILED.getValue());
                    k.setErrorMsg(e.getMessage());
                    knowledgeBaseRepository.save(k);
                });
            }
        });
    }

    /**
     * RAG 知识检索
     */
    public List<Document> retrieve(String query, int topK) {
        return vectorStore.similaritySearch(
            SearchRequest.builder().query(query).topK(topK).build()
        );
    }

    public List<KnowledgeBase> listAll() {
        return knowledgeBaseRepository.findAllByOrderByCreatedAtDesc();
    }

    public void delete(Long id) {
        knowledgeBaseRepository.deleteById(id);
    }

    private void ensureBucketExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String generateMinioPath(String originalFilename) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = cn.hutool.core.util.IdUtil.fastSimpleUUID();
        String ext = getFileExtension(originalFilename);
        return String.format("%s/%s.%s", date, uuid, ext);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return AppConstants.FILE_DEFAULT_EXTENSION;
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
