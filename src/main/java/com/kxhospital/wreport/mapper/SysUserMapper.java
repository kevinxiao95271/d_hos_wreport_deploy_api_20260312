package com.kxhospital.wreport.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 查询 sys_user / sys_role 表（只读，不改表结构）。
 *
 * 机构信息取链路：
 *   sys_user.person_id → hr_person.person_id → hr_person.org_id → hr_organization.org_name
 * 不直接使用 sys_user.org_id，保持与人员档案表的一致性。
 */
@Mapper
public interface SysUserMapper {

    /**
     * 按账号查登录用户，同时通过 person_id → hr_person → hr_organization
     * 取出 org_id 和 org_name，一次查询完成，避免多次往返。
     */
    @Select("SELECT u.user_id AS id, u.account, u.password, u.real_name, u.person_id, " +
            "       p.org_id, o.org_name " +
            "FROM sys_user u " +
            "LEFT JOIN hr_person p ON p.person_id = u.person_id AND p.del_flag = 'N' " +
            "LEFT JOIN hr_organization o ON o.org_id = p.org_id " +
            "WHERE u.account = #{account} " +
            "  AND u.del_flag = 'N' " +
            "  AND u.status_flag = 1 " +
            "LIMIT 1")
    Map<String, Object> findByAccount(@Param("account") String account);

    /**
     * 查用户角色码，用于登录后写入 JWT。
     */
    @Select("SELECT r.role_code FROM sys_user_role ur " +
            "JOIN sys_role r ON r.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} " +
            "LIMIT 1")
    String findRoleCode(@Param("userId") Long userId);

    /**
     * 查询所有可分配给任务的机构用户列表。
     * <p>链路：sys_user → hr_person → hr_organization，不使用 sys_user.org_id。</p>
     * <p>过滤条件：角色为 qcUser、账号未删除且已启用、hr_person 存在对应人员档案。</p>
     * <p>供 GET /wr/org/list 接口使用，前端在"分配机构"弹窗中加载可选项。</p>
     */
    @Select("SELECT u.user_id AS \"userId\", u.account, u.real_name AS \"realName\", " +
            "       p.org_id AS \"orgId\", o.org_name AS \"orgName\" " +
            "FROM sys_user u " +
            "JOIN sys_user_role ur ON ur.user_id = u.user_id " +
            "JOIN sys_role r       ON r.role_id  = ur.role_id AND r.role_code = 'qcUser' " +
            "JOIN hr_person p      ON p.person_id = u.person_id AND p.del_flag = 'N' " +
            "JOIN hr_organization o ON o.org_id   = p.org_id " +
            "WHERE u.del_flag = 'N' AND u.status_flag = 1 " +
            "ORDER BY o.org_name")
    List<Map<String, Object>> listOrgUsers();

    /**
     * 按机构 ID 列表查 qcUser 用户（orgIds 为空/null 时返回全部可分配机构用户）。
     */
    @org.apache.ibatis.annotations.Select("<script>" +
            "SELECT DISTINCT u.user_id AS \"userId\", p.org_id AS \"orgId\" " +
            "FROM sys_user u " +
            "JOIN sys_user_role ur ON ur.user_id = u.user_id " +
            "JOIN sys_role r ON r.role_id = ur.role_id AND r.role_code = 'qcUser' " +
            "JOIN hr_person p ON p.person_id = u.person_id AND p.del_flag = 'N' " +
            "WHERE u.del_flag = 'N' AND u.status_flag = 1 " +
            "<if test='orgIds != null and orgIds.size() &gt; 0'>" +
            "AND p.org_id IN " +
            "<foreach collection='orgIds' item='oid' open='(' separator=',' close=')'>#{oid}</foreach> " +
            "</if>" +
            "ORDER BY p.org_id, u.user_id" +
            "</script>")
    List<Map<String, Object>> listQcUserIdsByOrgIds(@Param("orgIds") List<Long> orgIds);
}
