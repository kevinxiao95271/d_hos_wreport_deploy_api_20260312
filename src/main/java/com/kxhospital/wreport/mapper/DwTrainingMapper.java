package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwTraining;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwTrainingMapper extends BaseMapper<DwTraining> {
    @Select("SELECT * FROM dw_training WHERE record_id = #{recordId} AND del_flag = 0 ORDER BY training_time ASC")
    List<DwTraining> listByRecord(@Param("recordId") Long recordId);
}
