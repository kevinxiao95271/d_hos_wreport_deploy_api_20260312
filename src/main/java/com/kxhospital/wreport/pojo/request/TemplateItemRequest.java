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
}
