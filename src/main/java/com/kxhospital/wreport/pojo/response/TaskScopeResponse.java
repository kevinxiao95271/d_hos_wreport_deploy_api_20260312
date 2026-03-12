package com.kxhospital.wreport.pojo.response;

import lombok.Data;

import java.util.List;

@Data
public class TaskScopeResponse {
    private Long taskId;
    private List<Long> orgIds;
}
