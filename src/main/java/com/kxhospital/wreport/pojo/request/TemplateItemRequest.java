package com.kxhospital.wreport.pojo.request;

import lombok.Data;

@Data
public class TemplateItemRequest {
    private Long   parentId;
    private String itemName;
    private Integer headerRow;
    private Integer colIndex;
    private Integer rowSpan;
    private Integer colSpan;
    private Integer isLeaf;
    private String  valueType;
    private String  unit;
    private String  placeholder;
    private Integer requireAttachment;
    private Integer sortNum;
    /**
     * 绑定字典编码（对应 wr_dict_type.dict_code）。
     * 传 null 或不传 → 普通输入框；传有效 dictCode → 下拉选择框。
     * 支持随时切换，不影响已有填报数据（存储层均为字符串，不做强校验）。
     */
    private String  dictCode;
}
