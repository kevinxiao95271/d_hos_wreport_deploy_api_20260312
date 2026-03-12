package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板行定义（用于附件3类"行固定+列可配"的勾选矩阵模板）
 * row_index 与 wr_record_value.row_index 对应，前端按此渲染行标签和层级缩进
 */
@Data
@TableName("wr_template_row")
public class WrTemplateRow implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long   templateId;

    /** 对应 wr_record_value.row_index，全模板唯一 */
    private Integer rowIndex;

    /** 行显示名称，如 "杭州市"、"上城区" */
    private String  rowLabel;

    /**
     * 行层级：1=省级汇总行（加粗），2=市级，3=县/区级
     * 前端据此控制缩进深度
     */
    private Integer rowLevel;

    /** 父行的 row_index（用于建立层级关系，省级汇总行为 null） */
    private Integer parentRowIndex;

    private Integer sortNum;

    @TableField(fill = FieldFill.INSERT)      private Long          createUser;
    @TableField(fill = FieldFill.INSERT)      private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private Long        updateUser;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
    @TableLogic(value = "0", delval = "1")   private Integer       delFlag;
}
