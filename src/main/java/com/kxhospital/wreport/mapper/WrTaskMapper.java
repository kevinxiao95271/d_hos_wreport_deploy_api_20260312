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
}
