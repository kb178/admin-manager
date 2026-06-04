package com.ddd.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回的 Token 信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDTO {

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** Token 类型 */
    private String tokenType;

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserInfoDTO userInfo;
}
