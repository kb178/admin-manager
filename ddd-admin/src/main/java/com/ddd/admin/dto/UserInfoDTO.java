package com.ddd.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * 登录成功后返回的用户信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoDTO {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 */
    private String avatar;

    /** 性别 */
    private Integer gender;

    /** 角色编码列表 */
    private Set<String> roles;

    /** 权限标识列表 */
    private Set<String> permissions;
}
