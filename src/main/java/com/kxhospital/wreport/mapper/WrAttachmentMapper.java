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

    /**
     * 统计某 record 下指定 item 的有效附件数。
     * 用于上传时校验 max_attachments：超出则拒绝。
     */
    @Select("SELECT COUNT(*) FROM wr_attachment WHERE record_id = #{recordId} AND item_id = #{itemId} AND del_flag = 0")
    int countByRecordAndItem(@Param("recordId") Long recordId, @Param("itemId") Long itemId);

    /**
     * 统计某 record 下指定 item 的有效附件数（不限 del_flag，用于 minAttachments 提交校验）。
     * 复用 countByRecordAndItem，del_flag=0 为有效附件。
     */
    @Select("SELECT * FROM wr_attachment WHERE record_id = #{recordId} AND item_id = #{itemId} AND del_flag = 0")
    List<WrAttachment> listByRecordAndItem(@Param("recordId") Long recordId, @Param("itemId") Long itemId);
}
