package com.kxhospital.wreport.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.pojo.request.RecordSaveRequest;
import com.kxhospital.wreport.pojo.request.RecordSubmitRequest;
import com.kxhospital.wreport.pojo.request.RecordAuditRequest;
import com.kxhospital.wreport.pojo.response.CrossViewVO;
import com.kxhospital.wreport.pojo.response.RecordAggregateResponse;
import com.kxhospital.wreport.pojo.response.RecordDetailVO;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

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

    /**
     * 实时字数统计，供前端显示进度条。
     * 返回 currentChars / maxTotalChars / enabled 三个字段。
     */
    Map<String, Object> charCount(Long recordId, LoginUser user);

    /**
     * 跨机构横向视图。
     * @param taskId     任务ID
     * @param itemIds    标准模板：选中展示的叶子列ID（null=全部）；矩阵模板忽略
     * @param rowIndexes 矩阵模板：选中展示的地理行rowIndex（null=全部）；标准模板忽略
     */
    CrossViewVO crossView(Long taskId, List<Long> itemIds, List<Integer> rowIndexes);
}
