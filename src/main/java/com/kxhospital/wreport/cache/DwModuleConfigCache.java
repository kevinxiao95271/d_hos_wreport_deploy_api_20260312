package com.kxhospital.wreport.cache;

import com.kxhospital.wreport.entity.DwFieldConfig;
import com.kxhospital.wreport.entity.DwModuleConfig;
import com.kxhospital.wreport.mapper.DwFieldConfigMapper;
import com.kxhospital.wreport.mapper.DwModuleConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 模块配置 + 扩展字段定义缓存。
 * 启动时从数据库加载一次，常驻内存；管理员更新配置后调用 refresh() 刷新。
 * 消除每次 detail / listModules 调用时对这两张准静态表的 DB 查询。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DwModuleConfigCache implements ApplicationRunner {

    private final DwModuleConfigMapper moduleConfigMapper;
    private final DwFieldConfigMapper  fieldConfigMapper;

    /** 全量模块（含禁用，供管理员使用） */
    private volatile List<DwModuleConfig> allModules = Collections.emptyList();
    /** 仅启用模块，供机构用户使用 */
    private volatile List<DwModuleConfig> enabledModules = Collections.emptyList();
    /** 所有启用扩展字段，按 moduleKey 分组 */
    private volatile Map<String, List<DwFieldConfig>> fieldsByModule = Collections.emptyMap();
    /** 所有启用扩展字段的平铺列表 */
    private volatile List<DwFieldConfig> allEnabledFields = Collections.emptyList();

    @Override
    public void run(ApplicationArguments args) {
        refresh();
    }

    /**
     * 重新从 DB 加载，管理员更新模块/字段配置后调用。
     */
    public synchronized void refresh() {
        log.info("[DwModuleConfigCache] 加载模块配置...");
        allModules     = moduleConfigMapper.listAll();
        enabledModules = allModules.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsEnabled()))
                .collect(Collectors.toList());

        List<DwFieldConfig> fields = fieldConfigMapper.listAllEnabled();
        allEnabledFields = fields;
        fieldsByModule   = fields.stream().collect(Collectors.groupingBy(DwFieldConfig::getModuleKey));

        log.info("[DwModuleConfigCache] 加载完成：modules={}, enabledModules={}, fields={}",
                allModules.size(), enabledModules.size(), fields.size());
    }

    public List<DwModuleConfig> getAllModules()     { return allModules; }
    public List<DwModuleConfig> getEnabledModules() { return enabledModules; }
    public List<DwFieldConfig>  getAllEnabledFields(){ return allEnabledFields; }

    /** moduleKey → 该模块下所有启用扩展字段（排好序）*/
    public Map<String, List<DwFieldConfig>> getFieldsByModule() { return fieldsByModule; }

    /** 某模块下所有启用的扩展字段 key 集合（用于 saveFieldValues 校验） */
    public java.util.Set<String> getValidFieldKeys(String moduleKey) {
        List<DwFieldConfig> list = fieldsByModule.getOrDefault(moduleKey, Collections.emptyList());
        return list.stream().map(DwFieldConfig::getFieldKey).collect(Collectors.toSet());
    }

    /** 返回某模块的满分上限；moduleKey 不存在时返回 null */
    public java.math.BigDecimal getScoreMax(String moduleKey) {
        return allModules.stream()
                .filter(m -> moduleKey.equals(m.getModuleKey()))
                .findFirst()
                .map(DwModuleConfig::getScoreMax)
                .orElse(null);
    }
}
