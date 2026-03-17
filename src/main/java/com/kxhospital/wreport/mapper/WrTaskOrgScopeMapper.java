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
     * 返回任务范围内每个机构的名称及当前填报状态。
     * orgName 取自 wr_record.org_name（填报时冗余存储的机构名），
     * 未开始填报的机构 orgName/recordStatus 均为 null。
     * hr_organization 仅含行政区划，质控中心不在其中，故不做该 JOIN。
     */
    @Select("SELECT s.org_id AS orgId, " +
            "  COALESCE(r.org_name, any_r.org_name) AS orgName, " +
            "  r.status AS recordStatus " +
            "FROM wr_task_org_scope s " +
            "LEFT JOIN wr_record r " +
            "  ON r.task_id = s.task_id AND r.org_id = s.org_id AND r.del_flag = 0 " +
            "LEFT JOIN LATERAL (" +
            "  SELECT org_name FROM wr_record " +
            "  WHERE org_id = s.org_id AND del_flag = 0 LIMIT 1" +
            ") any_r ON true " +
            "WHERE s.task_id = #{taskId} " +
            "ORDER BY COALESCE(r.org_name, any_r.org_name) NULLS LAST")
    @Results(id = "scopeOrgMap", value = {
            @Result(property = "orgId",       column = "orgId"),
            @Result(property = "orgName",     column = "orgName"),
            @Result(property = "recordStatus",column = "recordStatus")
    })
    List<TaskScopeOrgVO> selectScopeWithStatus(@Param("taskId") Long taskId);
}
