package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwDataAnalysis;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwDataAnalysisMapper extends BaseMapper<DwDataAnalysis> {
    @Select("SELECT * FROM dw_data_analysis WHERE record_id = #{recordId} AND del_flag = 0 " +
            "ORDER BY report_date ASC NULLS LAST, create_time ASC, id ASC")
    List<DwDataAnalysis> listByRecord(@Param("recordId") Long recordId);

    @Select("<script>SELECT record_id AS rid, COUNT(*) AS cnt FROM dw_data_analysis " +
            "WHERE record_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND del_flag = 0 GROUP BY record_id</script>")
    List<Map<String, Object>> countByRecordIds(@Param("ids") List<Long> ids);
}
