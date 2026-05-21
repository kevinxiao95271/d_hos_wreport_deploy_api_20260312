package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.QcMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QcMessageMapper extends BaseMapper<QcMessage> {

    @Select("SELECT COUNT(1) FROM qc_message WHERE del_flag = 'N' AND message_type = 3 " +
            "AND business_type = #{businessType} AND business_id = #{businessId} " +
            "AND target_type = 1 AND target_id = #{targetId}")
    int countActiveUserTodo(@Param("businessType") String businessType,
                            @Param("businessId") Long businessId,
                            @Param("targetId") Long targetId);
}
