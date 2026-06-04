package com.ddd.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户创建/更新 DTO
 */
@Data
public class UserCreateDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度为3-50个字符")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度为6-100位")
    private String password;

    /** 真实姓名 */
    @Size(max = 50, message = "真实姓名不超过50个字符")
    private String realName;

    /** 邮箱 */
    @Pattern(regexp = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$|^$", message = "邮箱格式不正确")
    private String email;

    /** 手机号 */
    @Pattern(regexp = "^1[3-9]\\d{9}$|^$", message = "手机号格式不正确")
    private String phone;

    /** 性别: 0-未知, 1-男, 2-女 */
    private Integer gender;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 部门ID */
    private Long deptId;

    /** 备注 */
    @Size(max = 500, message = "备注不超过500个字符")
    private String remark;

    /** 角色ID列表 */
    private java.util.List<Long> roleIds;
}
