package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 日常工作模块配置表
 * 管理员可调整：模块名、满分值、上传提示语、启用状态、显示顺序。
 * score_rule 仅管理员可见，机构端不展示。
 */
@Data
@TableName("dw_module_config")
public class DwModuleConfig implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    /** 模块唯一标识，与 dw_attachment.module_type 保持一致 */
    private String moduleKey;
    /** 前端展示名，可在管理后台修改 */
    private String moduleName;
    /** 该模块满分（含加分项），可调整 */
    private BigDecimal scoreMax;
    /** 公开版考核说明（机构可见），来自评分表原文，去掉具体分值 */
    private String scoreDesc;
    /** 完整评分规则（仅管理员可见），含具体得分数字 */
    private String scoreRule;
    /** 是否启用该模块（false 时前端不展示） */
    private Boolean isEnabled;
    /** 在填报页中的显示顺序 */
    private Integer sortOrder;
    /** 附件/填报上传提示语，展示给机构用户 */
    private String uploadHint;

    /**
     * 父模块 key（NULL=顶级节点或加分项）。
     * 大类容器节点的 parent_module_key 为 NULL；叶子子项指向所属大类的 module_key。
     */
    private String parentModuleKey;

    /**
     * 是否为叶子节点（可填报/上传）。
     * FALSE=大类容器，不填报，仅做分组展示；TRUE=实际填报项（默认）。
     */
    private Boolean isLeaf;

    /**
     * 是否为加分项。
     * TRUE=加分项（bonus_pub/bonus_comp/bonus_admin 等），独立分组展示。
     */
    private Boolean isBonus;

    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
