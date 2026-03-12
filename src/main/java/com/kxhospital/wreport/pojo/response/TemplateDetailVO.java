package com.kxhospital.wreport.pojo.response;

import com.kxhospital.wreport.entity.WrTemplate;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.entity.WrTemplateRow;
import lombok.Data;

import java.util.List;

/**
 * 模板完整详情：基本信息 + 列定义 + 行定义（矩阵类模板专用）
 */
@Data
public class TemplateDetailVO {
    private WrTemplate           template;
    private List<WrTemplateItem> items;
    /** 行定义，仅 value_type=checkbox 类模板有值，其他模板为空列表 */
    private List<WrTemplateRow>  rows;
}
