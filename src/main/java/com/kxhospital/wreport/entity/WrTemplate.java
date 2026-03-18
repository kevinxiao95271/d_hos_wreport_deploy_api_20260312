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
