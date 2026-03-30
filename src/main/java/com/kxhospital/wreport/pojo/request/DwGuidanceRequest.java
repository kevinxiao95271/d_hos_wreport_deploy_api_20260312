package com.kxhospital.wreport.pojo.request;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DwGuidanceRequest {
    private Long id;
    private Long recordId;
    private LocalDate guidanceStartDate;
    /** AM/PM */
    private String guidanceStartHalf;
    private LocalDate guidanceEndDate;
    /** AM/PM */
    private String guidanceEndHalf;
    /** 'online' | 'onsite' */
    private String guidanceForm;
    private String guidanceContent;
    /** 市级质控中心数量（省→市 两级树末级节点自动统计） */
    private Integer cityCenterCount;
    /** 市级质控中心选中节点ID列表（JSON数组字符串，供前端回显） */
    private String cityCenterIds;
    /** 县级质控中心数量（省→市→县 三级树末级节点自动统计） */
    private Integer countyCenterCount;
    /** 县级质控中心选中节点ID列表（JSON数组字符串，供前端回显） */
    private String countyCenterIds;
    /** 医疗机构数量（手动填入） */
    private Integer hospitalCount;
}
