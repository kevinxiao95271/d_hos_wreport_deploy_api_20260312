package com.kxhospital.wreport.pojo.response;

import lombok.Data;

import java.util.List;

/** 地区树节点（用于质控指导树形选择器） */
@Data
public class RegionNodeVO {

    private Integer id;
    private String  name;
    /** 子节点，叶节点时为 null */
    private List<RegionNodeVO> children;
}
