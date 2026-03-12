package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrTemplateItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WrTemplateItemMapper extends BaseMapper<WrTemplateItem> {
    @Select("SELECT * FROM wr_template_item WHERE template_id = #{templateId} AND del_flag = 0 ORDER BY header_row, col_index")
    List<WrTemplateItem> selectByTemplateId(@Param("templateId") Long templateId);
}
