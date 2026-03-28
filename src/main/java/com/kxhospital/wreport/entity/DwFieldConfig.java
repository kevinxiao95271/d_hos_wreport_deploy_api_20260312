package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模块扩展字段定义表
 * 运营/管理员在后台新增普通字段（文字/数字/枚举/多选），无需改代码。
 *
 * field_type 枚举：
 *   'text'     → 单行文本输入框
 *   'number'   → 数字输入框
 *   'enum'     → 下拉/单选，选项存于 fieldOptions（JSON字符串数组）
 *   'checkbox' → 多选框，选项存于 fieldOptions（JSON字符串数组）
 */
@Data
@TableName("dw_field_config")
public class DwFieldConfig implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    /** 关联 dw_module_config.module_key */
    private String moduleKey;
    /** 字段唯一标识（英文小写下划线，如 budget_amount） */
    private String fieldKey;
    /** 字段显示名（如"本次经费（元）"） */
    private String fieldName;
    /** 'text' | 'number' | 'enum' | 'checkbox' */
    private String fieldType;
    /** enum/checkbox 选项，JSON字符串数组，如 ["线上","线下"] */
    private String fieldOptions;
    private Boolean isRequired;
    private Integer sortOrder;
    private String placeholder;
    private Boolean isEnabled;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
