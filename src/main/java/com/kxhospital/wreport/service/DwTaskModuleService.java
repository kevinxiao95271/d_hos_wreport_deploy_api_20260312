package com.kxhospital.wreport.service;

import java.util.List;
import java.util.Set;

/**
 * 任务模块范围服务：控制每个 daily_work 任务启用哪些填报模块。
 * - scope 为空时降级返回全部启用模块（兼容未配置的历史任务）
 */
public interface DwTaskModuleService {

    /** 返回该任务当前启用的 moduleKey 集合（有序） */
    Set<String> resolveEnabledModuleKeys(Long taskId);

    /**
     * 按 dw_module_config.sort_order 返回任务启用的叶子模块 key，与填报页展示顺序一致。
     */
    List<String> listEnabledModuleKeysInDisplayOrder(Long taskId);

    /** 替换任务的模块范围（管理员配置用） */
    void replaceModuleScope(Long taskId, List<String> moduleKeys);

    /** 断言该模块在当前任务中已启用，否则抛 BusinessException(400) */
    void assertModuleAllowed(Long taskId, String moduleKey);

    /** 断言附件所属模块已启用（bonus 类型做特殊归并处理） */
    void assertAttachmentModuleAllowed(Long taskId, String moduleType);

    /** 断言 bonusType 对应的加分项模块已启用 */
    void assertBonusTypeAllowed(Long taskId, String bonusType);
}
