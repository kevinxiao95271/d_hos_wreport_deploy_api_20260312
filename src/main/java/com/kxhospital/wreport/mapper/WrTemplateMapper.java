package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WrTemplateMapper extends BaseMapper<WrTemplate> {
    IPage<WrTemplate> selectPage(Page<WrTemplate> page,
                                 @Param("templateName") String templateName,
                                 @Param("status") Integer status);

    @Select("SELECT * FROM wr_template WHERE status = 1 AND del_flag = 0 ORDER BY create_time DESC")
    List<WrTemplate> selectActiveList();
}
