package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    @Override
    public List<DwModuleConfigVO> listModules(boolean isAdmin) {
        List<DwModuleConfig> modules = isAdmin
                ? moduleConfigMapper.listAll()
                : moduleConfigMapper.listEnabled();

        // 一次性拉取所有启用字段定义，按 moduleKey 分组
        List<DwFieldConfig> allFields = fieldConfigMapper.listAllEnabled();
        Map<String, List<DwFieldConfig>> fieldsByModule = allFields.stream()
                .collect(Collectors.groupingBy(DwFieldConfig::getModuleKey));

        return modules.stream().map(m -> {
            DwModuleConfigVO vo = new DwModuleConfigVO();
            BeanUtils.copyProperties(m, vo);
            // 机构用户不返回分值相关字段（scoreMax / scoreRule），scoreDesc 公开可见
            if (!isAdmin) {
                vo.setScoreMax(null);
                vo.setScoreRule(null);
            }

            List<DwFieldConfig> fields = fieldsByModule.getOrDefault(m.getModuleKey(), Collections.emptyList());
            vo.setExtraFields(fields.stream().map(f -> {
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
    }

    @Override
    @Transactional
    public DwFieldConfig addField(DwFieldConfig config) {
        validateFieldType(config.getFieldType());
        if (config.getSortOrder() == null) config.setSortOrder(0);
        if (config.getIsRequired() == null) config.setIsRequired(false);
        if (config.getIsEnabled() == null) config.setIsEnabled(true);
        fieldConfigMapper.insert(config);
        return config;
    }

    @Override
    @Transactional
    public void updateField(DwFieldConfig config) {
        if (config.getId() == null) throw new BusinessException(400, "字段ID不能为空");
        if (config.getFieldType() != null) validateFieldType(config.getFieldType());
        fieldConfigMapper.updateById(config);
    }

    @Override
    @Transactional
    public void deleteField(Long fieldId) {
        DwFieldConfig fc = fieldConfigMapper.selectById(fieldId);
        if (fc == null) return;
        // 软删：disable 而不是物理删除，保留历史值
        fc.setIsEnabled(false);
        fieldConfigMapper.updateById(fc);
    }

    @Override
    @Transactional
    public void saveFieldValues(Long recordId, String moduleKey, Long subRecordId,
                                Map<String, String> values, LoginUser user) {
        if (values == null || values.isEmpty()) return;
        // 校验 field_key 均存在于配置里
        List<DwFieldConfig> validFields = fieldConfigMapper.listByModule(moduleKey);
        Set<String> validKeys = validFields.stream()
                .map(DwFieldConfig::getFieldKey)
                .collect(Collectors.toSet());

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String fk = entry.getKey();
            if (!validKeys.contains(fk))
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
}
