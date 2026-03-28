package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwGuidance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwGuidanceMapper extends BaseMapper<DwGuidance> {
    @Select("SELECT * FROM dw_guidance WHERE record_id = #{recordId} AND del_flag = 0 ORDER BY guidance_time ASC")
    List<DwGuidance> listByRecord(@Param("recordId") Long recordId);
}
