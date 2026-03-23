package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("wr_template")
public class WrTemplate implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    private String templateName;
    private String description;
    private Integer status;
    /**
     * 模板类型：
     * <ul>
     *   <li>"form"（默认）→ 原有表单录入模板（附件2/附件3，文字/数字录入 + 可选附件）</li>
     *   <li>"score"       → 评分细则模板（纯文件上传，每个指标有分值和文件数量约束）</li>
     * </ul>
     * 存量数据 template_type 列默认值为 'form'，无需迁移。
     */
    private String templateType;

    /**
     * 填报总字数上限（统计所有 wr_record_value.cell_value 的字符总数）。
     * <ul>
     *   <li>0（默认）→ 功能未启用，提交时不做字数校验</li>
     *   <li>正整数   → 功能启用，超出时拒绝提交并返回错误码 4032</li>
     * </ul>
     * 由管理员在模板配置页设置；前端通过 checkbox（是否启用）+ 数字框（上限值）组合操作。
     */
    private Integer maxTotalChars;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Long updateUser;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
}
