package com.kxhospital.wreport.service;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.WrTask;

import java.util.List;

/** 向质控平台 qc_message 写入待办（项目 B 首页展示） */
public interface WrTodoMessageService {

    /**
     * 任务发布后，向指定机构范围内的 qcUser 发送待办。
     * orgIds 为空表示任务未限定机构（全量可见），则通知全部可分配机构用户。
     */
    void sendTaskPublishTodos(WrTask task, List<Long> orgIds, LoginUser sender);

    /**
     * 已发布任务新增机构范围时，向新增机构用户补发待办（跳过已存在待办的用户）。
     */
    void sendTaskTodosForAddedOrgs(WrTask task, List<Long> addedOrgIds, LoginUser sender);

    /** 机构用户提交上报后，将本任务对应待办标记为已处理 */
    void markTaskTodoHandled(Long taskId, Long userId);
}
