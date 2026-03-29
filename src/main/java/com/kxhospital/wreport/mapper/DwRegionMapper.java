package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwRegion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwRegionMapper extends BaseMapper<DwRegion> {

    /** 按树类型查询所有节点，按 sort_order 排序 */
    @Select("SELECT * FROM dw_region WHERE tree_type = #{treeType} ORDER BY sort_order")
    List<DwRegion> listByTreeType(String treeType);
}
