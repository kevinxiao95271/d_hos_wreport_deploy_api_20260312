package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
}
