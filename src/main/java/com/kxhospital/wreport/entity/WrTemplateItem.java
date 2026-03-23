package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("wr_template_item")
public class WrTemplateItem implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long templateId;
    private Long parentId;
    private String itemName;
    private Integer headerRow;
    private Integer colIndex;
    private Integer rowSpan;
    private Integer colSpan;
    private Integer isLeaf;
    private String valueType;
    private String unit;
    private String placeholder;
    private Integer requireAttachment;
    private Long formatTemplateFileId;
    private String formatTemplateUrl;
    private String formatTemplateName;
    private Integer sortNum;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Long updateUser;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;

    /**
     * 绑定的字典编码（对应 wr_dict_type.dict_code）。
     * 非空时前端渲染下拉选择框，为空时按 valueType 渲染普通输入框。
     */
    private String dictCode;

    /**
     * 该指标最少上传文件数（仅 score 类模板有效）。
     * <ul>
     *   <li>0（默认）→ 不强制校验</li>
     *   <li>正整数   → 提交时附件数不足则拒绝，返回错误码 4033</li>
     * </ul>
     */
    private Integer minAttachments;

    /**
     * 该指标最多上传文件数（仅 score 类模板有效）。
     * <ul>
     *   <li>0（默认）→ 不限制数量</li>
     *   <li>正整数   → 上传时超出则直接拒绝</li>
     * </ul>
     */
    private Integer maxAttachments;

    /**
     * 该指标固定分值（仅 score 类模板使用，form 类默认 0）。
     * 导出时按指标文件是否达到 minAttachments 折算得分；无需用户填写。
     */
    private java.math.BigDecimal scoreValue;

    /**
     * 允许上传的文件格式，逗号分隔的扩展名（不含点），NULL 或空串表示不限制。
     * <p>示例：
     * <ul>
     *   <li>{@code "pdf"}              → 仅允许 PDF</li>
     *   <li>{@code "pdf,doc,docx"}     → PDF 或 Word</li>
     *   <li>{@code "pdf,jpg,jpeg,png,gif"} → PDF 或图片</li>
     * </ul>
     * 上传时后端取文件扩展名与此列表匹配（大小写不敏感），不符合则返回错误码 4035。
     * </p>
     */
    private String allowedFormats;

    /** 从根到本节点的名称路径，例如 ["2024年","学术会议","线上次数"]，不持久化，由 Service 计算后填充 */
    @TableField(exist = false)
    private List<String> headerPath;

    /**
     * 子节点列表，不持久化，由 Service 建树后填充。
     * <p>调用 GET /wr/template/items/{templateId} 时后端已按 sort_num 排好序并建好树，
     * 前端直接按列表顺序渲染即可，无需自行重建树结构。</p>
     */
    @TableField(exist = false)
    private List<WrTemplateItem> children;
}
