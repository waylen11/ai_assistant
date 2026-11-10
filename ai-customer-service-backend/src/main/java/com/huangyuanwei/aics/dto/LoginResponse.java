package com.huangyuanwei.aics.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@Builder
public class LoginResponse {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String avatarUrl;
    private Boolean permModel;
    private Boolean permKnowledge;
    private Boolean permUser;
}
