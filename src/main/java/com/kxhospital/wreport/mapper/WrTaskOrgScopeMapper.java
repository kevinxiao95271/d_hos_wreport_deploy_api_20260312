package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrTaskOrgScope;
import com.kxhospital.wreport.pojo.response.TaskScopeOrgVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WrTaskOrgScopeMapper extends BaseMapper<WrTaskOrgScope> {

    @Delete("DELETE FROM wr_task_org_scope WHERE task_id = #{taskId}")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT org_id FROM wr_task_org_scope WHERE task_id = #{taskId} ORDER BY org_id")
    List<Long> selectOrgIdsByTaskId(@Param("taskId") Long taskId);

    void insertBatch(@Param("list") List<WrTaskOrgScope> list);

    /**
     * 返回任务范围内每个机构的名称及当前填报状态（LEFT JOIN wr_record）。
     * recordStatus = null 表示该机构尚未开始填报。
     */
    @Select("SELECT s.org_id AS orgId, ho.org_name AS orgName, r.status AS recordStatus " +
            "FROM wr_task_org_scope s " +
            "LEFT JOIN hr_organization ho ON ho.org_id = s.org_id " +
            "LEFT JOIN wr_record r ON r.task_id = s.task_id AND r.org_id = s.org_id AND r.del_flag = 0 " +
            "WHERE s.task_id = #{taskId} " +
            "ORDER BY ho.org_name")
    @Results(id = "scopeOrgMap", value = {
            @Result(property = "orgId",       column = "orgId"),
            @Result(property = "orgName",     column = "orgName"),
            @Result(property = "recordStatus",column = "recordStatus")
    })
    List<TaskScopeOrgVO> selectScopeWithStatus(@Param("taskId") Long taskId);
}
