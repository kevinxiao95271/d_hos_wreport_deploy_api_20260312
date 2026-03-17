package com.kxhospital.wreport.pojo.response;

import lombok.Data;

/**
 * 任务范围内单个机构的快照：含机构信息及当前填报状态。
 * recordStatus: null=未开始, 0=草稿, 1=已提交, 2=已审核, 3=已驳回
 */
@Data
public class TaskScopeOrgVO {
    private Long    orgId;
    private String  orgName;
    /** 该机构在本任务的填报状态，null 表示尚未创建记录 */
    private Integer recordStatus;
}
