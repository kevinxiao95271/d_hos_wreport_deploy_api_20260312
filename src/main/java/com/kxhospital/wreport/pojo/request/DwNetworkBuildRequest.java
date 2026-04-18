package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 保存三级质控网络完善数据请求。
 * cityCenterIds / countyCenterIds 由前端树选择器生成，格式为 JSON 数组字符串，
 * 如 "[101,103,105]"；count 由前端统计末级选中节点数后传入。
 */
@Data
public class DwNetworkBuildRequest {

    private Long recordId;

    /** 已建立市级质控中心数量 */
    private Integer cityCenterCount;

    /** 已建立市级质控中心 ID 列表（JSON 数组字符串） */
    private String cityCenterIds;

    /** 已建立区县级质控中心数量 */
    private Integer countyCenterCount;

    /** 已建立区县级质控中心 ID 列表（JSON 数组字符串） */
    private String countyCenterIds;

    /** 机构自评分（可选） */
    private BigDecimal selfScore;
}
