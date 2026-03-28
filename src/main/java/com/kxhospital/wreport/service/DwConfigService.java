package com.kxhospital.wreport.service;

import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.DwFieldConfig;
import com.kxhospital.wreport.entity.DwModuleConfig;
import com.kxhospital.wreport.pojo.response.DwModuleConfigVO;

import java.util.List;
import java.util.Map;

/** 日常工作模块配置服务 */
public interface DwConfigService {

    /**
     * 获取所有模块配置（含扩展字段定义）
     * isAdmin=true 时包含 scoreRule；机构用户时 scoreRule 置 null
     */
    List<DwModuleConfigVO> listModules(boolean isAdmin);

    /** 更新模块配置（管理员） */
    void updateModule(DwModuleConfig config);

    /** 新增扩展字段定义（管理员） */
    DwFieldConfig addField(DwFieldConfig config);

    /** 更新扩展字段定义（管理员） */
    void updateField(DwFieldConfig config);

    /** 删除扩展字段定义（管理员，同时清除已有值） */
    void deleteField(Long fieldId);

    /**
     * 批量保存扩展字段值（upsert）
     *
     * @param recordId     填报记录ID
     * @param moduleKey    模块标识
     * @param subRecordId  子记录ID，单条/纯上传型模块传 null
     * @param values       { fieldKey -> fieldValue }
     * @param user         当前操作人
     */
    void saveFieldValues(Long recordId, String moduleKey, Long subRecordId,
                         Map<String, String> values, LoginUser user);

    /**
     * 读取某 record 全部扩展字段值
     * 返回结构：{ "moduleKey|subRecordId" -> { fieldKey -> fieldValue } }
     * subRecordId 为 null 时 key 为 "moduleKey|null"
     */
    Map<String, Map<String, String>> loadAllValues(Long recordId);
}
