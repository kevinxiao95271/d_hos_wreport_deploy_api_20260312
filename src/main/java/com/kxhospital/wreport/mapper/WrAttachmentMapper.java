package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WrAttachmentMapper extends BaseMapper<WrAttachment> {

    @Select("SELECT * FROM wr_attachment WHERE record_id = #{recordId} AND del_flag = 0")
    List<WrAttachment> selectByRecordId(@Param("recordId") Long recordId);

    @Select("SELECT COUNT(*) FROM wr_attachment WHERE record_id = #{recordId} AND del_flag = 0")
    int countByRecordId(@Param("recordId") Long recordId);
}
