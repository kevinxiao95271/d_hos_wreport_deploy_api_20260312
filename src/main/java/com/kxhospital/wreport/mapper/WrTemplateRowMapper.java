package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrTemplateRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface WrTemplateRowMapper extends BaseMapper<WrTemplateRow> {

    @Select("SELECT * FROM wr_template_row WHERE template_id = #{templateId} AND del_flag = 0 ORDER BY sort_num, row_index")
    List<WrTemplateRow> selectByTemplateId(@Param("templateId") Long templateId);

    @Update("UPDATE wr_template_row SET del_flag = 1 WHERE template_id = #{templateId}")
    void deleteByTemplateId(@Param("templateId") Long templateId);
}
