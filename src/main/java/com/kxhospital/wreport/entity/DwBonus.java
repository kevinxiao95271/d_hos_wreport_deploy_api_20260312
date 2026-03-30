package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 加分项（每种类型每个 record 最多一条）
 * bonus_type:
 *   'publication' = 丛书/指南/规范/共识
 *     pub_category: 'book_guide_consensus'=丛书/指南/共识(3分) | 'standard_norm'=标准/规范(2分)
 *   'competition'  = 技能竞赛
 *     comp_sponsor: 'provincial_joint'=省总工会+省卫健委联合(5分) | 'other'=其他形式(2分)
 */
@Data
@TableName("dw_bonus")
public class DwBonus implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    /** 'publication'=丛书/指南  'competition'=技能竞赛 */
    private String bonusType;
    private String pubName;
    /** 'book_guide_consensus'=丛书/指南/共识  'standard_norm'=标准/规范 */
    private String pubCategory;
    private LocalDate pubDate;
    private String compName;
    /** 'provincial_joint'=省总工会+省卫健委联合  'other'=其他形式 */
    private String compSponsor;
    private LocalDate compStartDate;
    /** AM/PM */
    private String compStartHalf;
    private LocalDate compEndDate;
    /** AM/PM */
    private String compEndHalf;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;
}
