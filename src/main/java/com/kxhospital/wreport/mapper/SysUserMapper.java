package com.kxhospital.wreport.mapper;

import org.apache.ibatis.annotations.*;


import java.util.List;
import java.util.Map;

/**
 * 查询已有的 sys_user / sys_role 表（只读，不改表结构）
 * Roses/Guns 7.x 实际列名:
 *   sys_user: user_id, account, password, real_name, org_id, status_flag(char), del_flag(char)
 *   sys_role:  role_id, role_code
 *   sys_user_role: user_id, role_id
 */
@Mapper
public interface SysUserMapper {

    @Select("SELECT user_id AS id, account, password, real_name, org_id " +
            "FROM sys_user " +
            "WHERE account = #{account} " +
            "  AND del_flag = 'N' " +
            "  AND status_flag = 1 " +
            "LIMIT 1")
    Map<String, Object> findByAccount(@Param("account") String account);

    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON r.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "LIMIT 1")
    String findRoleCode(@Param("userId") Long userId);

    /**
     * 查机构名：sys_user.org_id -> hr_organization.org_id -> org_name
     */
    @Select("SELECT org_name FROM hr_organization WHERE org_id = #{orgId} LIMIT 1")
    String findOrgName(@Param("orgId") Long orgId);

    // ---- 调试接口 ----
    @Select("SELECT tablename FROM pg_tables WHERE schemaname = current_schema() ORDER BY tablename")
    List<String> listTables();

    @Select("SELECT column_name FROM information_schema.columns " +
            "WHERE table_schema = current_schema() AND table_name = 'sys_user' " +
            "ORDER BY ordinal_position")
    List<String> listUserColumns();

    @Update("UPDATE sys_user SET password = #{password} WHERE account = #{account}")
    void updatePassword(@Param("account") String account, @Param("password") String password);

    @Select("SELECT u.user_id AS \"userId\", u.account, " +
            "       u.real_name AS \"realName\", u.org_id AS \"orgId\", " +
            "       ho.org_name AS \"orgName\", u.status_flag AS \"statusFlag\" " +
            "FROM sys_user u " +
            "LEFT JOIN hr_organization ho ON ho.org_id = u.org_id " +
            "WHERE u.del_flag = 'N' " +
            "ORDER BY u.create_time DESC LIMIT 20")
    List<Map<String, Object>> listRecentUsers();
}
