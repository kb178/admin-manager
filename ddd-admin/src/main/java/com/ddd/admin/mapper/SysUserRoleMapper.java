package com.ddd.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddd.admin.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关系 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
