package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwFieldConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwFieldConfigMapper extends BaseMapper<DwFieldConfig> {

    @Select("SELECT * FROM dw_field_config WHERE module_key = #{moduleKey} AND is_enabled = TRUE ORDER BY sort_order ASC")
    List<DwFieldConfig> listByModule(@Param("moduleKey") String moduleKey);

    @Select("SELECT * FROM dw_field_config WHERE is_enabled = TRUE ORDER BY module_key ASC, sort_order ASC")
    List<DwFieldConfig> listAllEnabled();
}
