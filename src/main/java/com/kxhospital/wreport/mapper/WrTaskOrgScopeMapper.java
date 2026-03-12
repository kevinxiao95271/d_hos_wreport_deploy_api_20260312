package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrTaskOrgScope;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WrTaskOrgScopeMapper extends BaseMapper<WrTaskOrgScope> {

    @Delete("DELETE FROM wr_task_org_scope WHERE task_id = #{taskId}")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Select("SELECT org_id FROM wr_task_org_scope WHERE task_id = #{taskId} ORDER BY org_id")
    List<Long> selectOrgIdsByTaskId(@Param("taskId") Long taskId);

    void insertBatch(@Param("list") List<WrTaskOrgScope> list);
}
