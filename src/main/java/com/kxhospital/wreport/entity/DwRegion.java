package com.kxhospital.wreport.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 质控指导地区树节点（浙江省市/县两棵树）
 * tree_type: 'city'   = 省→市级质控中心，共 11 个叶节点
 *            'county' = 省→市→区县，市节点 level=2，区县节点 level=3
 */
@Data
@TableName("dw_region")
public class DwRegion implements Serializable {

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    /** 父节点 ID，市/叶节点为 null */
    private Integer parentId;

    private String name;

    /** 2=市，3=区县 */
    private Short level;

    /** 'city' | 'county' */
    private String treeType;

    private Integer sortOrder;
}
