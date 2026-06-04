package com.ddd.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ddd.admin.entity.SysUserRole;

/**
 * 用户角色关系 Service
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    /**
     * 给用户分配角色
     */
    void assignRoles(Long userId, java.util.List<Long> roleIds, Long operatorId);
}
