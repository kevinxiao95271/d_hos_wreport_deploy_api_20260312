package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwSurvey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwSurveyMapper extends BaseMapper<DwSurvey> {
    @Select("SELECT * FROM dw_survey WHERE record_id = #{recordId} AND del_flag = 0 ORDER BY survey_time ASC")
    List<DwSurvey> listByRecord(@Param("recordId") Long recordId);
}
