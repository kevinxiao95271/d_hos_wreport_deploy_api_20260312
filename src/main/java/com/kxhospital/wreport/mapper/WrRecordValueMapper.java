package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrRecordValue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface WrRecordValueMapper extends BaseMapper<WrRecordValue> {

    @Select("SELECT * FROM wr_record_value WHERE record_id = #{recordId}")
    List<WrRecordValue> selectByRecordId(@Param("recordId") Long recordId);

    /** PostgreSQL upsert */
    void batchUpsert(@Param("list") List<WrRecordValue> list);

    @Delete("DELETE FROM wr_record_value WHERE record_id = #{recordId}")
    void deleteByRecordId(@Param("recordId") Long recordId);

    /** 批量按 recordIds 查所有值（crossview 用，一次查完减少 N+1） */
    @Select("<script>SELECT * FROM wr_record_value WHERE record_id IN " +
            "<foreach collection='recordIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<WrRecordValue> selectByRecordIds(@Param("recordIds") List<Long> recordIds);
}
