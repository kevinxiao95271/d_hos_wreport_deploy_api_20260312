package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwFieldValue;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface DwFieldValueMapper extends BaseMapper<DwFieldValue> {

    /** 查询某 record 下所有扩展字段值（含子记录级） */
    @Select("SELECT * FROM dw_field_value WHERE record_id = #{recordId}")
    List<DwFieldValue> listByRecord(@Param("recordId") Long recordId);

    /** 查询某子记录下某模块的扩展字段值 */
    @Select("SELECT * FROM dw_field_value WHERE sub_record_id = #{subRecordId} AND module_key = #{moduleKey}")
    List<DwFieldValue> listBySubRecord(@Param("subRecordId") Long subRecordId,
                                       @Param("moduleKey") String moduleKey);

    /** 查询 record 级（subRecordId IS NULL）某模块的扩展字段值 */
    @Select("SELECT * FROM dw_field_value WHERE record_id = #{recordId} AND module_key = #{moduleKey} AND sub_record_id IS NULL")
    List<DwFieldValue> listByRecordModule(@Param("recordId") Long recordId,
                                          @Param("moduleKey") String moduleKey);

    /** 查找已有值（用于 upsert 前判断） */
    @Select("<script>" +
            "SELECT * FROM dw_field_value WHERE record_id=#{recordId} AND module_key=#{moduleKey} AND field_key=#{fieldKey}" +
            "<if test='subRecordId != null'> AND sub_record_id=#{subRecordId}</if>" +
            "<if test='subRecordId == null'> AND sub_record_id IS NULL</if>" +
            " LIMIT 1</script>")
    DwFieldValue findExisting(@Param("recordId") Long recordId,
                              @Param("subRecordId") Long subRecordId,
                              @Param("moduleKey") String moduleKey,
                              @Param("fieldKey") String fieldKey);

    /** 删除某子记录下所有扩展字段值（级联删除子记录时使用） */
    @Delete("DELETE FROM dw_field_value WHERE sub_record_id = #{subRecordId}")
    void deleteBySubRecord(@Param("subRecordId") Long subRecordId);
}
