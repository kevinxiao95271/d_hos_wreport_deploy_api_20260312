package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DwAttachmentMapper extends BaseMapper<DwAttachment> {

    @Select("SELECT * FROM dw_attachment WHERE record_id = #{recordId} AND del_flag = 0 ORDER BY create_time ASC")
    List<DwAttachment> listByRecord(@Param("recordId") Long recordId);

    @Select("SELECT * FROM dw_attachment WHERE sub_record_id = #{subRecordId} AND module_type = #{moduleType} AND del_flag = 0 ORDER BY create_time ASC")
    List<DwAttachment> listBySubRecord(@Param("subRecordId") Long subRecordId, @Param("moduleType") String moduleType);

    @Select("SELECT * FROM dw_attachment WHERE record_id = #{recordId} AND module_type = #{moduleType} AND sub_record_id IS NULL AND del_flag = 0 ORDER BY create_time ASC")
    List<DwAttachment> listByRecordAndModule(@Param("recordId") Long recordId, @Param("moduleType") String moduleType);

    @Select("SELECT COUNT(*) FROM dw_attachment WHERE sub_record_id = #{subRecordId} AND module_type = #{moduleType} AND slot = #{slot} AND del_flag = 0")
    int countBySubSlot(@Param("subRecordId") Long subRecordId, @Param("moduleType") String moduleType, @Param("slot") String slot);

    @Select("SELECT COUNT(*) FROM dw_attachment WHERE record_id = #{recordId} AND module_type = #{moduleType} AND slot = #{slot} AND sub_record_id IS NULL AND del_flag = 0")
    int countByRecordSlot(@Param("recordId") Long recordId, @Param("moduleType") String moduleType, @Param("slot") String slot);

    /** 批量统计各 record 下纯上传型模块的附件数（sub_record_id IS NULL） */
    @Select("<script>SELECT record_id AS rid, module_type AS mk, COUNT(*) AS cnt FROM dw_attachment " +
            "WHERE del_flag = 0 AND sub_record_id IS NULL AND record_id IN " +
            "<foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "GROUP BY record_id, module_type</script>")
    List<Map<String, Object>> countUploadModulesByRecordIds(@Param("ids") List<Long> ids);
}
