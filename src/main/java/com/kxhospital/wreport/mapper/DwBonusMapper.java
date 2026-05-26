package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwBonus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwBonusMapper extends BaseMapper<DwBonus> {
    @Select("SELECT * FROM dw_bonus WHERE record_id = #{recordId} AND del_flag = 0")
    List<DwBonus> listByRecord(@Param("recordId") Long recordId);

    @Select("<script>SELECT record_id AS rid, COUNT(*) AS cnt FROM dw_bonus " +
            "WHERE record_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY record_id</script>")
    List<Map<String, Object>> countByRecordIds(@Param("ids") List<Long> ids);

    @Select("<script>SELECT record_id AS rid, COUNT(*) AS cnt FROM dw_bonus " +
            "WHERE del_flag = 0 AND bonus_type = #{bonusType} AND record_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY record_id</script>")
    List<Map<String, Object>> countByRecordIdsAndBonusType(@Param("ids") List<Long> ids,
                                                           @Param("bonusType") String bonusType);
}
