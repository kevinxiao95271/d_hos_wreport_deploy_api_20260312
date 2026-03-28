package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwFunding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface DwFundingMapper extends BaseMapper<DwFunding> {
    @Select("SELECT * FROM dw_funding WHERE record_id = #{recordId} AND del_flag = 0 LIMIT 1")
    DwFunding findByRecord(@Param("recordId") Long recordId);
}
