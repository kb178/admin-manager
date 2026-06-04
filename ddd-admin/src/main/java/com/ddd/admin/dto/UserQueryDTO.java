package com.ddd.admin.dto;

import lombok.Data;

/**
 * 用户查询 DTO
 */
@Data
public class UserQueryDTO {

    /** 用户名（模糊查询） */
    private String username;

    /** 真实姓名（模糊查询） */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 部门ID */
    private Long deptId;

    /** 当前页（默认1） */
    private Integer page = 1;

    /** 每页条数（默认10） */
    private Integer pageSize = 10;
}
