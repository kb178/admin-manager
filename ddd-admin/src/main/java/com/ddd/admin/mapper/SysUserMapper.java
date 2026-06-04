package com.ddd.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddd.admin.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 查询用户的角色编码列表
     */
    @Select("""
        SELECT r.role_code FROM sys_user_role ur
        INNER JOIN sys_role r ON ur.role_id = r.id AND r.is_deleted = 0 AND r.status = 1
        WHERE ur.user_id = #{userId}
        """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 根据角色编码查询用户ID列表
     */
    @Select("""
        SELECT DISTINCT u.id FROM sys_user u
        INNER JOIN sys_user_role ur ON u.id = ur.user_id
        INNER JOIN sys_role r ON ur.role_id = r.id
        WHERE r.role_code = #{roleCode} AND u.is_deleted = 0 AND r.is_deleted = 0
        """)
    List<Long> selectUserIdsByRoleCode(@Param("roleCode") String roleCode);
}
