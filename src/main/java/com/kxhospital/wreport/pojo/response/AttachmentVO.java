package com.kxhospital.wreport.pojo.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AttachmentVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   recordId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long   itemId;
    private String attachName;
    private String attachPath;
    private Long   attachSize;
    private String attachType;
    private LocalDateTime createTime;

    /** 叶子节点名称，itemId=null 时为"整体附件" */
    private String       itemName;
    /**
     * 完整祖先路径（含自身），用于前端展示节点上下文。
     * 整体附件 → ["整体附件"]
     * 叶子节点 → ["一级表头", "二级表头", ..., "叶子名称"]
     */
    private List<String> headerPath;
}
