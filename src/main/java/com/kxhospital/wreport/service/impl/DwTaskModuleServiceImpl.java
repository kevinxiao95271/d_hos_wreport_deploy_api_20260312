package com.kxhospital.wreport.service.impl;

import com.kxhospital.wreport.cache.DwModuleConfigCache;
import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.common.DwTaskModuleKeys;
import com.kxhospital.wreport.entity.DwModuleConfig;
import com.kxhospital.wreport.entity.DwTaskModuleScope;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.mapper.DwTaskModuleScopeMapper;
import com.kxhospital.wreport.mapper.WrTaskMapper;
import com.kxhospital.wreport.service.DwTaskModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DwTaskModuleServiceImpl implements DwTaskModuleService {

    private final DwTaskModuleScopeMapper scopeMapper;
    private final DwModuleConfigCache     moduleConfigCache;
    private final WrTaskMapper            taskMapper;

    @Override
    public Set<String> resolveEnabledModuleKeys(Long taskId) {
        if (taskId == null) return defaultEnabledKeys();
        WrTask t = taskMapper.selectById(taskId);
        if (t == null || !"daily_work".equals(t.getTaskType())) return defaultEnabledKeys();

        LinkedHashSet<String> resolved = new LinkedHashSet<>();
        List<String> keys = scopeMapper.selectModuleKeysByTaskId(taskId);
        if (keys != null && !keys.isEmpty()) {
            resolved.addAll(keys);
        } else {
            resolved.addAll(defaultEnabledKeys());
        }

        // 兼容旧任务：scope 创建时模块列表不完整，自动补齐标准模块
        if (t.getStatQuarter() == null) {
            resolved.addAll(DwTaskModuleKeys.ANNUAL_MODULES);
            DwTaskModuleKeys.ANNUAL_EXCLUDED_MODULES.forEach(resolved::remove);
        } else {
            resolved.addAll(DwTaskModuleKeys.QUARTER_MODULES);
        }

        Set<String> validEnabled = moduleConfigCache.getEnabledModules().stream()
                .map(DwModuleConfig::getModuleKey)
                .collect(Collectors.toSet());
        resolved.retainAll(validEnabled);
        return resolved;
    }

    private Set<String> defaultEnabledKeys() {
        return moduleConfigCache.getEnabledModules().stream()
                .map(DwModuleConfig::getModuleKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    @Transactional
    public void replaceModuleScope(Long taskId, List<String> moduleKeys) {
        scopeMapper.deleteByTaskId(taskId);
        if (moduleKeys == null || moduleKeys.isEmpty()) return;
        Set<String> valid = moduleConfigCache.getAllModules().stream()
                .map(DwModuleConfig::getModuleKey).collect(Collectors.toSet());
        int order = 0;
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (String k : moduleKeys) {
            if (k == null || k.trim().isEmpty()) continue;
            String key = k.trim();
            if (!valid.contains(key)) throw new BusinessException(400, "未知模块标识：" + key);
            if (!seen.add(key)) continue;
            DwTaskModuleScope row = new DwTaskModuleScope();
            row.setTaskId(taskId);
            row.setModuleKey(key);
            row.setSortOrder(order++);
            row.setCreateTime(now);
            scopeMapper.insert(row);
        }
        if (seen.isEmpty()) throw new BusinessException(400, "请至少选择一个有效模块");
    }

    @Override
    public void assertModuleAllowed(Long taskId, String moduleKey) {
        if (moduleKey == null) return;
        if (!resolveEnabledModuleKeys(taskId).contains(moduleKey))
            throw new BusinessException(400, "当前任务未启用该填报模块：" + moduleKey);
    }

    @Override
    public void assertAttachmentModuleAllowed(Long taskId, String moduleType) {
        if (moduleType == null) return;
        if ("bonus".equals(moduleType)) {
            Set<String> set = resolveEnabledModuleKeys(taskId);
            if (!set.contains("bonus_pub") && !set.contains("bonus_comp"))
                throw new BusinessException(400, "当前任务未启用加分项模块");
            return;
        }
        assertModuleAllowed(taskId, moduleType);
    }

    @Override
    public void assertBonusTypeAllowed(Long taskId, String bonusType) {
        if ("publication".equals(bonusType))  assertModuleAllowed(taskId, "bonus_pub");
        else if ("competition".equals(bonusType)) assertModuleAllowed(taskId, "bonus_comp");
    }
}
