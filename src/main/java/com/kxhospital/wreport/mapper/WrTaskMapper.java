package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WrTaskMapper extends BaseMapper<WrTask> {
    IPage<WrTask> selectPage(Page<WrTask> page,
                             @Param("taskName") String taskName,
                             @Param("status") Integer status);

    @Select("SELECT * FROM wr_task WHERE status = 1 AND del_flag = 0 ORDER BY create_time DESC")
    List<WrTask> selectActiveTasks();

    @Select("SELECT t.* FROM wr_task t WHERE t.status = 1 AND t.del_flag = 0 " +
            "AND (NOT EXISTS (SELECT 1 FROM wr_task_org_scope s WHERE s.task_id = t.id) " +
            "     OR EXISTS (SELECT 1 FROM wr_task_org_scope s WHERE s.task_id = t.id AND s.org_id = #{orgId})) " +
            "ORDER BY t.create_time DESC")
    List<WrTask> selectActiveTasksByOrg(@Param("orgId") Long orgId);

    @Select("SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM wr_task_org_scope WHERE task_id=#{taskId}) " +
            "THEN 1 " +
            "ELSE (SELECT COUNT(1) FROM wr_task_org_scope WHERE task_id=#{taskId} AND org_id=#{orgId}) END")
    int isTaskInScope(@Param("taskId") Long taskId, @Param("orgId") Long orgId);
}
