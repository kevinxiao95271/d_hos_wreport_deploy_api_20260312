package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwGuidance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwGuidanceMapper extends BaseMapper<DwGuidance> {
    @Select("SELECT * FROM dw_guidance WHERE record_id = #{recordId} AND del_flag = 0 ORDER BY guidance_start_date ASC, guidance_start_half ASC, id ASC")
    List<DwGuidance> listByRecord(@Param("recordId") Long recordId);

    @Select("<script>SELECT record_id AS rid, COUNT(*) AS cnt FROM dw_guidance " +
            "WHERE record_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY record_id</script>")
    List<Map<String, Object>> countByRecordIds(@Param("ids") List<Long> ids);
}
