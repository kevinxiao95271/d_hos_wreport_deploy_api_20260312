package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwTraining;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwTrainingMapper extends BaseMapper<DwTraining> {
    @Select("SELECT id, record_id, training_name, training_start_date, training_start_half, " +
            "training_end_date, training_end_half, training_form, training_content, " +
            "attendee_count AS training_people_count, del_flag, create_user, create_time, update_time " +
            "FROM dw_training WHERE record_id = #{recordId} AND del_flag = 0 " +
            "ORDER BY training_start_date ASC NULLS LAST, " +
            "CASE WHEN training_start_half IN ('PM', '下午') THEN 1 WHEN training_start_half IN ('AM', '上午') THEN 0 ELSE 0 END ASC, " +
            "create_time ASC, id ASC")
    List<DwTraining> listByRecord(@Param("recordId") Long recordId);

    @Select("<script>SELECT record_id AS rid, COUNT(*) AS cnt FROM dw_training " +
            "WHERE del_flag = 0 AND record_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY record_id</script>")
    List<Map<String, Object>> countByRecordIds(@Param("ids") List<Long> ids);
}
