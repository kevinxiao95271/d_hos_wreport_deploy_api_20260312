package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WrTemplateMapper extends BaseMapper<WrTemplate> {
    IPage<WrTemplate> selectPage(Page<WrTemplate> page,
                                 @Param("templateName") String templateName,
                                 @Param("status") Integer status);
}
