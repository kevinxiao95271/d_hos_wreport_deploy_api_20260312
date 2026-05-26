package com.kxhospital.wreport.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 只读查询 hr_organization（质控机构 organization_type=2）。
 */
@Mapper
public interface HrOrganizationMapper {

    /**
     * @param orgCategory null=质控中心+技术指导中心(1,2)；1=质控中心；2=技术指导中心
     */
    @Select("<script>" +
            "SELECT org_id AS \"orgId\", org_name AS \"orgName\", org_category AS \"orgCategory\" " +
            "FROM hr_organization " +
            "WHERE del_flag = 'N' AND organization_type = '2' " +
            "<choose>" +
            "  <when test='orgCategory != null'>AND org_category = CAST(#{orgCategory} AS VARCHAR)</when>" +
            "  <otherwise>AND org_category IN ('1', '2')</otherwise>" +
            "</choose>" +
            "ORDER BY org_name" +
            "</script>")
    List<Map<String, Object>> listQcOrgs(@Param("orgCategory") Integer orgCategory);

    @Select("<script>" +
            "SELECT org_id FROM hr_organization " +
            "WHERE del_flag = 'N' AND organization_type = '2' " +
            "<choose>" +
            "  <when test='orgCategory != null'>AND org_category = CAST(#{orgCategory} AS VARCHAR)</when>" +
            "  <otherwise>AND org_category IN ('1', '2')</otherwise>" +
            "</choose>" +
            "</script>")
    List<Long> selectQcOrgIds(@Param("orgCategory") Integer orgCategory);
}
