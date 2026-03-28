package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模块扩展字段值表
 * 所有字段类型统一以 TEXT 存储，前端根据 DwFieldConfig.fieldType 解析。
 * checkbox 类型存 JSON 数组字符串，如 ["opt1","opt3"]。
 *
 * subRecordId：
 *   多条记录型模块（meeting/training/guidance/survey/bonus）时为子记录ID；
 *   纯上传/单条型模块（annual_work/funding 等）时为 NULL。
 */
@Data
@TableName("dw_field_value")
public class DwFieldValue implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private Long subRecordId;
    private String moduleKey;
    private String fieldKey;
    private String fieldValue;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
