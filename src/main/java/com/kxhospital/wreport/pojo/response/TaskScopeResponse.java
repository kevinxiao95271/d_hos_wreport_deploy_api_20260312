package com.kxhospital.wreport.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class TaskScopeResponse {
    private Long                 taskId;
    /** 兼容旧字段（纯 id 列表） */
    private List<Long>           orgIds;
    /** 新增：含机构名称及填报状态，前端用于渲染 checkbox + 置灰逻辑 */
    private List<TaskScopeOrgVO> orgs;
}
