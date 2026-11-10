package com.huangyuanwei.aics.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 用户信息响应 DTO
 */
@Data
@Builder
public class UserInfoResponse {

    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Boolean permModel;
    private Boolean permKnowledge;
    private Boolean permUser;
}
