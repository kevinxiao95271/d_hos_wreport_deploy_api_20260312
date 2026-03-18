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

    /**
     * 统计单份填报的总字符数。
     * <p>仅统计 cell_value 非 NULL 的行；空字符串计 0 字符（LENGTH('') = 0）。</p>
     * <p>用途：
     * <ul>
     *   <li>提交时校验：与模板 max_total_chars 比较，超出则拒绝（error 4032）</li>
     *   <li>前端实时进度条：调用 GET /wr/record/charcount 实时返回当前字数</li>
     * </ul>
     * </p>
     */
    @Select("SELECT COALESCE(SUM(LENGTH(cell_value)), 0) FROM wr_record_value WHERE record_id = #{recordId}")
    long sumCharCount(@Param("recordId") Long recordId);
}
