package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwBonus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwBonusMapper extends BaseMapper<DwBonus> {
    @Select("SELECT * FROM dw_bonus WHERE record_id = #{recordId} AND del_flag = 0")
    List<DwBonus> listByRecord(@Param("recordId") Long recordId);
}
