package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.WrDictItem;
import com.kxhospital.wreport.entity.WrDictType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WrDictMapper extends BaseMapper<WrDictType> {

    @Select("SELECT * FROM wr_dict_type WHERE del_flag=0 ORDER BY id")
    List<WrDictType> selectAllTypes();

    @Select("SELECT * FROM wr_dict_item WHERE dict_type_id=#{typeId} AND del_flag=0 AND status=1 ORDER BY sort_num")
    List<WrDictItem> selectItemsByTypeId(@Param("typeId") Long typeId);

    @Select("SELECT i.* FROM wr_dict_item i JOIN wr_dict_type t ON t.id=i.dict_type_id " +
            "WHERE t.dict_code=#{dictCode} AND t.del_flag=0 AND i.del_flag=0 AND i.status=1 ORDER BY i.sort_num")
    List<WrDictItem> selectItemsByCode(@Param("dictCode") String dictCode);
}
