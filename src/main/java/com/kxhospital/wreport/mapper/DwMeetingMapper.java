package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kxhospital.wreport.entity.DwMeeting;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DwMeetingMapper extends BaseMapper<DwMeeting> {
    @Select("SELECT * FROM dw_meeting WHERE record_id = #{recordId} AND del_flag = 0 ORDER BY meeting_time ASC")
    List<DwMeeting> listByRecord(@Param("recordId") Long recordId);
}
