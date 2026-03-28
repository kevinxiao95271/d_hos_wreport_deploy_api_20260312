package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwModuleConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwModuleConfigMapper extends BaseMapper<DwModuleConfig> {

    @Select("SELECT * FROM dw_module_config ORDER BY sort_order ASC")
    List<DwModuleConfig> listAll();

    @Select("SELECT * FROM dw_module_config WHERE is_enabled = TRUE ORDER BY sort_order ASC")
    List<DwModuleConfig> listEnabled();
}
