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

    /** 从根到本节点的名称路径，例如 ["2024年","学术会议","线上次数"]，不持久化，由 Service 计算后填充 */
    @TableField(exist = false)
    private List<String> headerPath;
}
