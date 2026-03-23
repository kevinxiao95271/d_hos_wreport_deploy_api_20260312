package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrTemplateItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WrTemplateItemMapper extends BaseMapper<WrTemplateItem> {
    // 先按 sort_num 排，保证建树后兄弟节点顺序与模板设计一致
    @Select("SELECT * FROM wr_template_item WHERE template_id = #{templateId} AND del_flag = 0 ORDER BY sort_num ASC, col_index ASC")
    List<WrTemplateItem> selectByTemplateId(@Param("templateId") Long templateId);

    /** 软删除（del_flag=1），保留行以供审计 */
    @Update("UPDATE wr_template_item SET del_flag = 1 WHERE template_id = #{templateId}")
    void deleteByTemplateId(@Param("templateId") Long templateId);

    /** 物理删除，用于 replaceItems 时确保 PK 不冲突（可安全复用旧 id） */
    @Delete("DELETE FROM wr_template_item WHERE template_id = #{templateId}")
    void physicalDeleteByTemplateId(@Param("templateId") Long templateId);
}
