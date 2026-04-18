package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwNetworkBuild;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DwNetworkBuildMapper extends BaseMapper<DwNetworkBuild> {

    @Select("SELECT * FROM dw_network_build WHERE record_id = #{recordId} AND del_flag = 0 LIMIT 1")
    DwNetworkBuild findByRecord(@Param("recordId") Long recordId);
}
