package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwMeeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwMeetingMapper extends BaseMapper<DwMeeting> {
    @Select("SELECT * FROM dw_meeting WHERE record_id = #{recordId} AND del_flag = 0 " +
            "ORDER BY meeting_start_date ASC NULLS LAST, " +
            "CASE WHEN meeting_start_half IN ('PM', '下午') THEN 1 WHEN meeting_start_half IN ('AM', '上午') THEN 0 ELSE 0 END ASC, " +
            "create_time ASC, id ASC")
    List<DwMeeting> listByRecord(@Param("recordId") Long recordId);

    @Select("<script>SELECT record_id AS rid, COUNT(*) AS cnt FROM dw_meeting " +
            "WHERE record_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY record_id</script>")
    List<Map<String, Object>> countByRecordIds(@Param("ids") List<Long> ids);
}
