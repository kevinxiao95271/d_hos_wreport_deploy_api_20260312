package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kxhospital.wreport.cache.DwModuleConfigCache;
import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.DwFieldConfig;
import com.kxhospital.wreport.entity.DwFieldValue;
import com.kxhospital.wreport.entity.DwModuleConfig;
import com.kxhospital.wreport.mapper.DwFieldConfigMapper;
import com.kxhospital.wreport.mapper.DwFieldValueMapper;
import com.kxhospital.wreport.mapper.DwModuleConfigMapper;
import com.kxhospital.wreport.pojo.response.DwModuleConfigVO;
import com.kxhospital.wreport.service.DwConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DwConfigServiceImpl implements DwConfigService {

    private final DwModuleConfigMapper moduleConfigMapper;
    private final DwFieldConfigMapper  fieldConfigMapper;
    private final DwFieldValueMapper   fieldValueMapper;
    private final DwModuleConfigCache  moduleConfigCache;

    @Override
    public List<DwModuleConfigVO> listModules(boolean isAdmin) {
        // 直接读内存缓存，不访问 DB
        List<DwModuleConfig> modules = isAdmin
                ? moduleConfigCache.getAllModules()
                : moduleConfigCache.getEnabledModules();
        Map<String, List<DwFieldConfig>> fieldsByModule = moduleConfigCache.getFieldsByModule();

        return modules.stream().map(m -> {
            DwModuleConfigVO vo = new DwModuleConfigVO();
            BeanUtils.copyProperties(m, vo);
            // 机构用户可见 scoreMax（用于展示权重）；scoreRule 仍仅管理员可见
            if (!isAdmin) {
                vo.setScoreRule(null);
            }

        List<DwFieldConfig> fields = fieldsByModule.getOrDefault(m.getModuleKey(), Collections.emptyList());
        vo.setExtraFields(fields.stream().filter(f -> !"module_self_score".equals(f.getFieldKey())).map(f -> {
                DwModuleConfigVO.DwFieldConfigVO fvo = new DwModuleConfigVO.DwFieldConfigVO();
                BeanUtils.copyProperties(f, fvo);
                return fvo;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateModule(DwModuleConfig config) {
        if (config.getId() == null) throw new BusinessException(400, "模块ID不能为空");
        moduleConfigMapper.updateById(config);
        moduleConfigCache.refresh();   // 同步刷新缓存
    }

    @Override
    @Transactional
    public DwFieldConfig addField(DwFieldConfig config) {
        validateFieldType(config.getFieldType());
        if (config.getSortOrder() == null) config.setSortOrder(0);
        if (config.getIsRequired() == null) config.setIsRequired(false);
        if (config.getIsEnabled() == null) config.setIsEnabled(true);
        fieldConfigMapper.insert(config);
        moduleConfigCache.refresh();
        return config;
    }

    @Override
    @Transactional
    public void updateField(DwFieldConfig config) {
        if (config.getId() == null) throw new BusinessException(400, "字段ID不能为空");
        if (config.getFieldType() != null) validateFieldType(config.getFieldType());
        fieldConfigMapper.updateById(config);
        moduleConfigCache.refresh();
    }

    @Override
    @Transactional
    public void deleteField(Long fieldId) {
        DwFieldConfig fc = fieldConfigMapper.selectById(fieldId);
        if (fc == null) return;
        // 软删：disable 而不是物理删除，保留历史值
        fc.setIsEnabled(false);
        fieldConfigMapper.updateById(fc);
        moduleConfigCache.refresh();
    }

    @Override
    @Transactional
    public void saveFieldValues(Long recordId, String moduleKey, Long subRecordId,
                                Map<String, String> values, LoginUser user) {
        if (values == null || values.isEmpty()) return;
        // 从缓存校验 field_key 合法性，无需 DB 查询
        Set<String> validKeys = moduleConfigCache.getValidFieldKeys(moduleKey);

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String fk = entry.getKey();
            if (!validKeys.contains(fk) && !isAllowedDynamicField(moduleKey, fk))
                throw new BusinessException(400, "未知字段：" + fk + "，请刷新后重试");

            // select-then-insert/update 避免 ON CONFLICT 对 NULL 的限制
            DwFieldValue existing = fieldValueMapper.findExisting(recordId, subRecordId, moduleKey, fk);
            if (existing != null) {
                existing.setFieldValue(entry.getValue());
                fieldValueMapper.updateById(existing);
            } else {
                DwFieldValue fv = new DwFieldValue();
                fv.setRecordId(recordId);
                fv.setSubRecordId(subRecordId);
                fv.setModuleKey(moduleKey);
                fv.setFieldKey(fk);
                fv.setFieldValue(entry.getValue());
                fieldValueMapper.insert(fv);
            }
        }
    }

    @Override
    public Map<String, Map<String, String>> loadAllValues(Long recordId) {
        List<DwFieldValue> all = fieldValueMapper.listByRecord(recordId);
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (DwFieldValue fv : all) {
            String groupKey = fv.getModuleKey() + "|" + (fv.getSubRecordId() == null ? "null" : fv.getSubRecordId());
            result.computeIfAbsent(groupKey, k -> new LinkedHashMap<>())
                  .put(fv.getFieldKey(), fv.getFieldValue());
        }
        return result;
    }

    // ── 工具 ──────────────────────────────────────────────────────

    private void validateFieldType(String type) {
        Set<String> allowed = new HashSet<>(Arrays.asList("text", "number", "enum", "checkbox"));
        if (!allowed.contains(type))
            throw new BusinessException(400, "不支持的字段类型：" + type + "，允许值：text/number/enum/checkbox");
    }

    /** 国家/省报告：近3年勾选字段 checked_2026 等，按统计年度动态生成 */
    private boolean isAllowedDynamicField(String moduleKey, String fieldKey) {
        if (fieldKey == null) return false;
        if ("module_self_score".equals(fieldKey)) return true;
        if (!"national_report".equals(moduleKey) && !"prov_report".equals(moduleKey)) return false;
        return fieldKey.matches("checked_\\d{4}");
    }
}
