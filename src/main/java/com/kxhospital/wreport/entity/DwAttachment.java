package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 日常工作模块附件表
 * 文件实体存储于 MinIO，本表只存 URL + 元数据。
 *
 * module_type 枚举：
 *   'meeting'           质控会议
 *   'training'          质控培训
 *   'guidance'          质控指导
 *   'survey'            质控调研
 *   'annual_work'       年度工作落实推进
 *   'it_construction'   信息化建设
 *   'work_plan'         工作计划总结
 *   'admin_response'    行政指令响应与传达
 *   'activity_report'   质控活动报备
 *   'bonus'             加分项
 *
 * slot 枚举：
 *   meeting        → minutes | photo | signin
 *   training       → material | photo
 *   guidance       → evidence
 *   survey         → report | photo
 *   annual_work    → evidence
 *   it_construction→ evidence
 *   work_plan      → plan | summary
 *   admin_response → evidence
 *   activity_report→ pre_report | post_report
 *   bonus          → evidence
 */
@Data
@TableName("dw_attachment")
public class DwAttachment implements Serializable {
    @TableId(value = "id", type = IdType.ASSIGN_ID) private Long id;
    private Long recordId;
    private String moduleType;
    /** 多条记录型模块的子记录ID（dw_meeting/training/guidance/survey/bonus 的 id） */
    private Long subRecordId;
    private String slot;
    private String fileName;
    /** MinIO 直链 URL */
    private String fileUrl;
    private Long fileSize;
    private String fileMime;
    @TableLogic(value = "0", delval = "1") private Integer delFlag;
    @TableField(fill = FieldFill.INSERT) private Long createUser;
    @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
}
