package com.kxhospital.wreport.pojo.request;

import lombok.Data;

@Data
public class TemplateItemRequest {
    /**
     * 批次内唯一标识（client-side id）。
     * - 已有节点：填其真实 DB id（Long 的字符串形式，如 "2000000000000101"）
     * - 新增节点：前端生成任意唯一字符串（如 "new_1"、UUID）
     * - 后端在两阶段处理时：纯数字 → 复用该 DB id；非纯数字 → 生成新 Snowflake id
     */
    private String  id;

    /**
     * 父节点的 id 字段值（引用同批次某节点的 id），null 表示根节点。
     */
    private String  parentId;

    private String  itemName;
    private Integer headerRow;
    private Integer colIndex;
    private Integer rowSpan;
    private Integer colSpan;
    private Integer isLeaf;
    private String  valueType;
    private String  unit;
    private String  placeholder;
    private Integer requireAttachment;
    private Integer sortNum;
    /**
     * 绑定字典编码。null 或空串 → 普通输入框；有效 dictCode → 下拉选择框。
     */
    private String  dictCode;

    /**
     * 该指标最少上传文件数（score 类模板使用）。0 或 null = 不强制校验。
     */
    private Integer minAttachments;

    /**
     * 该指标最多上传文件数（score 类模板使用）。0 或 null = 不限。
     */
    private Integer maxAttachments;

    /**
     * 该指标固定分值（score 类模板使用）。导出时使用，用户无需填写。
     */
    private java.math.BigDecimal scoreValue;

    /**
     * 允许上传的文件格式，逗号分隔扩展名（不含点），null 或空串 = 不限制。
     * 示例："pdf"  /  "pdf,doc,docx"  /  "pdf,jpg,jpeg,png,gif"
     */
    private String allowedFormats;
}
