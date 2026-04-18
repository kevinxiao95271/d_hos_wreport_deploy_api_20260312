package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 三级质控网络完善（每个 wr_record 只有一条）
 * 机构通过两棵树（省→市 二级树、省→市→县 三级树）勾选已建立质控中心的单位范围。
 * 管理员对照覆盖情况自行评分，系统不自动计分。
 */
@Data
@TableName("dw_network_build")
public class DwNetworkBuild implements Serializable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long recordId;

    /** 已勾选的市级质控中心数量（由前端树统计后传入） */
    private Integer cityCenterCount;

    /** 已勾选的市级质控中心 ID 列表（JSON 数组，如 [101,103,105]） */
    private String cityCenterIds;

    /** 已勾选的区县级质控中心数量 */
    private Integer countyCenterCount;

    /** 已勾选的区县级质控中心 ID 列表（JSON 数组） */
    private String countyCenterIds;

    /** 机构自评分（可为空） */
    private BigDecimal selfScore;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
