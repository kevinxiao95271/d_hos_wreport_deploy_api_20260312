package com.kxhospital.wreport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface WrRecordMapper extends BaseMapper<WrRecord> {

    IPage<WrRecord> selectAdminPage(Page<WrRecord> page,
                                    @Param("taskId") Long taskId,
                                    @Param("orgName") String orgName,
                                    @Param("status") Integer status);

    IPage<WrRecord> selectMyPage(Page<WrRecord> page,
                                 @Param("orgId") Long orgId,
                                 @Param("taskId") Long taskId,
                                 @Param("status") Integer status);

    @Select("SELECT * FROM wr_record WHERE task_id = #{taskId} AND org_id = #{orgId} AND del_flag = 0 LIMIT 1")
    WrRecord findByTaskAndOrg(@Param("taskId") Long taskId, @Param("orgId") Long orgId);

    Map<String, Object> selectAggregate(@Param("taskId") Long taskId);
}
