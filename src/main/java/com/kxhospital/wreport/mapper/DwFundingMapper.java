package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwFunding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwFundingMapper extends BaseMapper<DwFunding> {
    @Select("SELECT * FROM dw_funding WHERE record_id = #{recordId} AND del_flag = 0 LIMIT 1")
    DwFunding findByRecord(@Param("recordId") Long recordId);

    /** 返回在 ids 范围内有经费记录的 record_id 集合 */
    @Select("<script>SELECT DISTINCT record_id FROM dw_funding " +
            "WHERE record_id IN <foreach item='id' collection='ids' open='(' separator=',' close=')'>#{id}</foreach> " +
            "AND del_flag = 0</script>")
    List<Long> existingRecordIds(@Param("ids") List<Long> ids);
}
