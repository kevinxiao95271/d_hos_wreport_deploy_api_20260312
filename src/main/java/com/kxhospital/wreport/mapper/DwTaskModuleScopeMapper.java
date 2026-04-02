package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwTaskModuleScope;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwTaskModuleScopeMapper extends BaseMapper<DwTaskModuleScope> {

    @Select("SELECT module_key FROM dw_task_module_scope WHERE task_id = #{taskId} ORDER BY sort_order")
    List<String> selectModuleKeysByTaskId(@Param("taskId") Long taskId);

    @Delete("DELETE FROM dw_task_module_scope WHERE task_id = #{taskId}")
    void deleteByTaskId(@Param("taskId") Long taskId);
}
