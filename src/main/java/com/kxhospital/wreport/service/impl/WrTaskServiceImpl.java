package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.entity.WrTaskOrgScope;
import com.kxhospital.wreport.mapper.WrTaskMapper;
import com.kxhospital.wreport.mapper.WrTaskOrgScopeMapper;
import com.kxhospital.wreport.service.WrTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WrTaskServiceImpl implements WrTaskService {

    private final WrTaskMapper taskMapper;
    private final WrTaskOrgScopeMapper scopeMapper;

    @Override
    public IPage<WrTask> page(Page<WrTask> page, String taskName, Integer status) {
        return taskMapper.selectPage(page, taskName, status);
    }

    @Override
    public WrTask detail(Long id) {
        return taskMapper.selectById(id);
    }

    @Override
    public Long add(WrTask task) {
        task.setStatus(0);
        taskMapper.insert(task);
        return task.getId();
    }

    @Override
    public void update(WrTask task) {
        taskMapper.updateById(task);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<WrTask> w = new LambdaUpdateWrapper<>();
        w.eq(WrTask::getId, id).set(WrTask::getStatus, status);
        taskMapper.update(null, w);
    }

    @Override
    public void delete(Long id) {
        taskMapper.deleteById(id);
        scopeMapper.deleteByTaskId(id);
    }

    @Override
    public List<WrTask> activeList(Long orgId) {
        return orgId != null ? taskMapper.selectActiveTasksByOrg(orgId) : taskMapper.selectActiveTasks();
    }

    public void replaceScope(Long taskId, List<Long> orgIds, LoginUser user) {
        scopeMapper.deleteByTaskId(taskId);
        if (orgIds == null || orgIds.isEmpty()) {
            return;
        }
        List<WrTaskOrgScope> scopes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long orgId : orgIds) {
            if (orgId == null) continue;
            WrTaskOrgScope scope = new WrTaskOrgScope();
            scope.setTaskId(taskId);
            scope.setOrgId(orgId);
            scope.setCreateUser(user != null ? user.getUserId() : null);
            scope.setCreateTime(now);
            scopes.add(scope);
        }
        if (!scopes.isEmpty()) {
            scopeMapper.insertBatch(scopes);
        }
    }

    public List<Long> getScopeOrgIds(Long taskId) {
        return scopeMapper.selectOrgIdsByTaskId(taskId);
    }

    public boolean isTaskInScope(Long taskId, Long orgId) {
        if (orgId == null) return false;
        return taskMapper.isTaskInScope(taskId, orgId) > 0;
    }
}
