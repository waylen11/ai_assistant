package com.huangyuanwei.aics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取盐值响应 DTO
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaltResponse {

    private String salt;
}
