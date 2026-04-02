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

    /** 某年度下所有 daily_work 季度任务，按 stat_quarter 排序（null 排最后） */
    @Select("SELECT * FROM wr_task WHERE task_type = 'daily_work' AND stat_year = #{statYear} " +
            "AND del_flag = 0 ORDER BY stat_quarter NULLS LAST, create_time ASC")
    List<WrTask> selectDailyWorkByStatYear(@Param("statYear") String statYear);
    IPage<WrTask> selectPage(Page<WrTask> page,
                             @Param("taskName") String taskName,
                             @Param("status") Integer status);

    @Select("SELECT * FROM wr_task WHERE status = 1 AND del_flag = 0 ORDER BY create_time DESC")
    List<WrTask> selectActiveTasks();

    // LEFT JOIN 代替双层相关子查询，消除逐行子查询；走 idx_wr_task_org_scope_tid 和复合索引
    @Select("SELECT t.* FROM wr_task t " +
            "LEFT JOIN wr_task_org_scope s ON s.task_id = t.id AND s.org_id = #{orgId} " +
            "WHERE t.status = 1 AND t.del_flag = 0 " +
            "AND (s.org_id IS NOT NULL " +
            "     OR NOT EXISTS (SELECT 1 FROM wr_task_org_scope x WHERE x.task_id = t.id)) " +
            "ORDER BY t.create_time DESC")
    List<WrTask> selectActiveTasksByOrg(@Param("orgId") Long orgId);

    // 走复合索引 idx_wr_task_org_scope_tid_oid
    @Select("SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM wr_task_org_scope WHERE task_id=#{taskId}) " +
            "THEN 1 " +
            "ELSE (SELECT COUNT(1) FROM wr_task_org_scope WHERE task_id=#{taskId} AND org_id=#{orgId}) END")
    int isTaskInScope(@Param("taskId") Long taskId, @Param("orgId") Long orgId);
}
