package com.kxhospital.wreport.pojo.request;

import lombok.Data;

import java.util.List;

@Data
public class TaskScopeRequest {
    private List<Long> orgIds;
}
