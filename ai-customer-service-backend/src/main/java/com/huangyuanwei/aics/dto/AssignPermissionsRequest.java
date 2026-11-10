package com.huangyuanwei.aics.dto;

import lombok.Data;

/**
 * 分配权限请求 DTO
 */
@Data
public class AssignPermissionsRequest {

    /** 模型管理权限 */
    private Boolean permModel = false;

    /** 知识库管理权限 */
    private Boolean permKnowledge = false;

    /** 用户管理权限 */
    private Boolean permUser = false;
}
