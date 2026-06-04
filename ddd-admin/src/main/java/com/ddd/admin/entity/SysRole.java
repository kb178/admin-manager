package com.ddd.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色表 — sys_role
 */
@Data
@TableName("sys_role")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 角色名称 */
    private String roleName;

    /** 角色编码 (如: SUPER_ADMIN, ADMIN, USER, AUDITOR) */
    private String roleCode;

    /** 排序（越小越靠前） */
    private Integer roleSort;

    /** 数据权限: 1-全部数据, 2-本级数据, 3-本级及子级, 4-仅本人数据 */
    private Integer dataScope;

    /** 状态: 0-禁用, 1-启用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 逻辑删除: 0-未删除, 1-已删除 */
    @TableLogic
    private Integer isDeleted;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /** 修改人ID */
    @TableField(fill = FieldFill.UPDATE)
    private Long updatedBy;

    /** 修改时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
