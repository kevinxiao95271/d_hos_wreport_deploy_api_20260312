package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.entity.WrTaskOrgScope;
import com.kxhospital.wreport.mapper.WrRecordMapper;
import com.kxhospital.wreport.mapper.WrTaskMapper;
import com.kxhospital.wreport.mapper.WrTaskOrgScopeMapper;
import com.kxhospital.wreport.pojo.response.TaskScopeOrgVO;
import com.kxhospital.wreport.service.WrTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrTaskServiceImpl implements WrTaskService {

    private final WrTaskMapper         taskMapper;
    private final WrTaskOrgScopeMapper scopeMapper;
    private final WrRecordMapper       recordMapper;

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

    /**
     * 替换任务范围。
     * 兜底校验：若被移除的机构已有 status>=1（已提交/审核/驳回）的填报记录，
     * 抛出 BusinessException(4031) 阻断操作，前端应先驳回再移出。
     */
    public void replaceScope(Long taskId, List<Long> orgIds, LoginUser user) {
        // ── 兜底：计算被移除的机构并检查是否有已提交记录 ──────────────────────
        List<Long> currentOrgIds = scopeMapper.selectOrgIdsByTaskId(taskId);
        if (!currentOrgIds.isEmpty()) {
            Set<Long> newSet = orgIds == null ? Collections.emptySet() : new HashSet<>(orgIds);
            List<Long> removedOrgIds = currentOrgIds.stream()
                    .filter(id -> !newSet.contains(id))
                    .collect(Collectors.toList());
            if (!removedOrgIds.isEmpty()) {
                Set<Long> removedSet = new HashSet<>(removedOrgIds);
                List<WrRecord> submittedAll = recordMapper.selectSubmittedByTask(taskId);
                List<WrRecord> blocked = submittedAll.stream()
                        .filter(r -> removedSet.contains(r.getOrgId()))
                        .collect(Collectors.toList());
                if (!blocked.isEmpty()) {
                    String names = blocked.stream()
                            .map(WrRecord::getOrgName)
                            .distinct()
                            .collect(Collectors.joining("、"));
                    throw new BusinessException(4031,
                            "以下机构已有提交/审核记录，请先驳回后再移出：" + names);
                }
            }
        }
        // ── 正常替换 ──────────────────────────────────────────────────────────
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

    /** 返回任务范围内机构列表（含机构名称和当前填报状态），供前端渲染 checkbox 置灰逻辑 */
    public List<TaskScopeOrgVO> getScopeWithStatus(Long taskId) {
        return scopeMapper.selectScopeWithStatus(taskId);
    }

    public boolean isTaskInScope(Long taskId, Long orgId) {
        if (orgId == null) return false;
        return taskMapper.isTaskInScope(taskId, orgId) > 0;
    }
}
