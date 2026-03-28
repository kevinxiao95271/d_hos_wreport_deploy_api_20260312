package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 质控指导记录（多条，关联 wr_record）
 * 被指导目标通过三种方式记录数量：
 *   市级质控中心：省→市 两级树勾选，末级节点数自动统计 → cityCenterCount
 *   县级质控中心：省→市→县 三级树勾选，末级节点数自动统计 → countyCenterCount
 *   医疗机构：手动填入 → hospitalCount
 * 三者之和不能为 0（由 DB CHECK 约束保证）
 */
@Data
@TableName("dw_guidance")
public class DwGuidance implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private LocalDate guidanceTime;
    /** 'online'=线上  'onsite'=现场 */
    private String guidanceForm;
    private String guidanceContent;
    private Integer cityCenterCount;
    /** 市级质控中心勾选节点ID列表（JSON数组字符串，供前端回显） */
    private String cityCenterIds;
    private Integer countyCenterCount;
    /** 县级质控中心勾选节点ID列表（JSON数组字符串，供前端回显） */
    private String countyCenterIds;
    private Integer hospitalCount;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
