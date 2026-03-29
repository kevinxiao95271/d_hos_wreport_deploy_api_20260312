package com.kxhospital.wreport.pojo.response;

import lombok.Data;

import java.util.List;

/**
 * 质控指导地区树返回体
 *
 * cityTree  — 省→市级质控中心（共 11 个叶节点，前端渲染平铺复选框）
 * countyTree — 省→市→区县（前端渲染两级树形选择器）
 *
 * 前端计数逻辑：
 *   cityCenterCount   = cityTree 中被勾选的节点数
 *   countyCenterCount = countyTree 中被勾选的 level=3 叶节点数
 */
@Data
public class GuidanceRegionsVO {

    /** 市级质控中心平铺列表（无 children） */
    private List<RegionNodeVO> cityTree;

    /** 省→市→区县两级树（市节点含 children） */
    private List<RegionNodeVO> countyTree;
}
