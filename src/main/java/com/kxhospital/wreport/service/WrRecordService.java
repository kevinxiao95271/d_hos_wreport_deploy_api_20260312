package com.kxhospital.wreport.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.pojo.request.RecordSaveRequest;
import com.kxhospital.wreport.pojo.request.RecordSubmitRequest;
import com.kxhospital.wreport.pojo.request.RecordAuditRequest;
import com.kxhospital.wreport.pojo.response.RecordAggregateResponse;
import com.kxhospital.wreport.pojo.response.RecordDetailVO;

import javax.servlet.http.HttpServletResponse;

public interface WrRecordService {
    Long saveOrUpdate(RecordSaveRequest req, LoginUser user);
    void submit(RecordSubmitRequest req, LoginUser user);
    IPage<WrRecord> adminPage(Page<WrRecord> page, Long taskId, String orgName, Integer status);
    IPage<WrRecord> myPage(Page<WrRecord> page, LoginUser user, Long taskId, Integer status);
    WrRecord myRecord(Long taskId, LoginUser user);
    RecordDetailVO detail(Long recordId);
    void audit(RecordAuditRequest req, LoginUser user);
    void exportExcel(Long taskId, HttpServletResponse response);
    RecordAggregateResponse aggregate(Long taskId);
}
