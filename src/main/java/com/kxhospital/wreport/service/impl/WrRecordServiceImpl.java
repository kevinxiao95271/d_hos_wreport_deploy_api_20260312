package com.kxhospital.wreport.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.entity.*;
import com.kxhospital.wreport.mapper.*;
import com.kxhospital.wreport.pojo.request.RecordAuditRequest;
import com.kxhospital.wreport.pojo.request.RecordSaveRequest;
import com.kxhospital.wreport.pojo.request.RecordSubmitRequest;
import com.kxhospital.wreport.pojo.response.AttachmentVO;
import com.kxhospital.wreport.pojo.response.RecordAggregateResponse;
import com.kxhospital.wreport.pojo.response.RecordDetailVO;
import com.kxhospital.wreport.service.WrAttachmentService;
import com.kxhospital.wreport.service.WrRecordService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrRecordServiceImpl implements WrRecordService {

    private final WrRecordMapper       recordMapper;
    private final WrRecordValueMapper  valueMapper;
    private final WrTaskMapper         taskMapper;
    private final WrTemplateItemMapper itemMapper;
    private final WrAttachmentService  attachmentService;
    private final WrAttachmentMapper   attachmentMapper;

    @Override
    @Transactional
    public Long saveOrUpdate(RecordSaveRequest req, LoginUser user) {
        WrTask task = taskMapper.selectById(req.getTaskId());
        if (task == null || task.getDelFlag() == 1) throw new RuntimeException("任务不存在");
        if (taskMapper.isTaskInScope(req.getTaskId(), user.getOrgId()) == 0) {
            throw new RuntimeException("任务未分配到当前机构");
        }

        String orgName = user.getRealName();
        // 查找或新建 record
        WrRecord record = recordMapper.findByTaskAndOrg(req.getTaskId(), user.getOrgId());
        if (record == null) {
            record = new WrRecord();
            record.setTaskId(req.getTaskId());
            record.setTemplateId(task.getTemplateId());
            record.setOrgId(user.getOrgId());
            record.setOrgName(orgName);
            record.setStatus(0); // 草稿
            recordMapper.insert(record);
        } else if (record.getStatus() == 2) {
            // 已审核通过，不允许再修改数据
            throw new RuntimeException("该记录已审核通过，无法修改");
        } else if (!Objects.equals(record.getOrgName(), orgName)) {
            record.setOrgName(orgName);
            recordMapper.updateById(record);
        }
        // status=3（驳回）：允许继续修改数据，status 本身不变（submit 时才切回 1）

        // 构建 value 列表
        Long recordId = record.getId();
        Long templateId = task.getTemplateId();
        List<WrRecordValue> toUpsert = buildValues(req, recordId, templateId);

        if (!toUpsert.isEmpty()) {
            valueMapper.batchUpsert(toUpsert);
        }
        return recordId;
    }

    @Override
    @Transactional
    public void submit(RecordSubmitRequest req, LoginUser user) {
        WrRecord record = recordMapper.selectById(req.getRecordId());
        if (record == null) throw new RuntimeException("上报记录不存在");
        if (!record.getOrgId().equals(user.getOrgId())) throw new RuntimeException("无权操作");
        if (record.getStatus() == 2) throw new RuntimeException("已审核通过，不可重复提交");

        if (taskMapper.isTaskInScope(record.getTaskId(), user.getOrgId()) == 0) {
            throw new RuntimeException("任务未分配到当前机构");
        }

        LocalDateTime now = LocalDateTime.now();

        if (record.getStatus() == 3) {
            // ---- 驳回后重提：校验 resubmitDeadline，与任务截止无关 ----
            LocalDateTime resubmitDeadline = record.getResubmitDeadline();
            if (resubmitDeadline != null && now.isAfter(resubmitDeadline)) {
                throw new RuntimeException("重提截止时间已过（" + resubmitDeadline + "），请联系管理员重新开放");
            }
        } else {
            // ---- 草稿首次提交：校验任务截止日期 ----
            WrTask task = taskMapper.selectById(record.getTaskId());
            if (task.getDeadline() != null && now.isAfter(task.getDeadline())) {
                throw new RuntimeException("已超过填报截止时间（" + task.getDeadline() + "）");
            }
        }

        record.setStatus(1);
        record.setSubmitUser(user.getUserId());
        record.setSubmitTime(now);
        recordMapper.updateById(record);
    }

    @Override
    public IPage<WrRecord> adminPage(Page<WrRecord> page, Long taskId, String orgName, Integer status) {
        return recordMapper.selectAdminPage(page, taskId, orgName, status);
    }

    @Override
    public IPage<WrRecord> myPage(Page<WrRecord> page, LoginUser user, Long taskId, Integer status) {
        return recordMapper.selectMyPage(page, user.getOrgId(), taskId, status);
    }

    @Override
    public WrRecord myRecord(Long taskId, LoginUser user) {
        return recordMapper.findByTaskAndOrg(taskId, user.getOrgId());
    }

    @Override
    public RecordDetailVO detail(Long recordId) {
        WrRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("记录不存在");
        List<WrRecordValue> values = valueMapper.selectByRecordId(recordId);
        List<AttachmentVO>  attachments = attachmentService.listByRecord(recordId);

        RecordDetailVO vo = new RecordDetailVO();
        vo.setRecord(record);
        vo.setValues(values);
        vo.setAttachments(attachments);
        vo.setStatusLabel(statusLabel(record.getStatus()));
        return vo;
    }

    /** 默认驳回后允许重提的天数 */
    private static final int DEFAULT_RESUBMIT_DAYS = 7;

    @Override
    @Transactional
    public void audit(RecordAuditRequest req, LoginUser user) {
        WrRecord record = recordMapper.selectById(req.getRecordId());
        if (record == null) throw new RuntimeException("记录不存在");
        if (record.getStatus() != 1) throw new RuntimeException("记录未处于待审核状态");

        boolean rejected = (req.getAuditResult() == 2);
        LocalDateTime now = LocalDateTime.now();

        record.setStatus(rejected ? 3 : 2);       // 2=审核通过  3=驳回
        record.setAuditResult(req.getAuditResult());
        record.setAuditRemark(req.getAuditRemark());
        record.setAuditUser(user.getUserId());
        record.setAuditTime(now);

        if (rejected) {
            // 驳回时设置重提截止：优先用管理员指定值，否则默认 +7 天
            LocalDateTime resubmitDeadline = req.getResubmitDeadline() != null
                    ? req.getResubmitDeadline()
                    : now.plusDays(DEFAULT_RESUBMIT_DAYS);
            record.setResubmitDeadline(resubmitDeadline);
        } else {
            // 通过时清空重提截止（若之前有过驳回记录）
            record.setResubmitDeadline(null);
        }

        recordMapper.updateById(record);
    }

    @Override
    public void exportExcel(Long taskId, HttpServletResponse response) {
        List<WrRecord> records = recordMapper.selectAdminPage(
                new Page<>(1, 10000), taskId, null, null).getRecords();

        List<ExportRow> rows = records.stream().map(r -> {
            ExportRow row = new ExportRow();
            row.setOrgName(r.getOrgName());
            row.setStatus(statusLabel(r.getStatus()));
            row.setSubmitTime(r.getSubmitTime() != null ? r.getSubmitTime().toString() : "");
            row.setAuditResult(r.getAuditResult() == null ? "" : r.getAuditResult() == 1 ? "通过" : "驳回");
            row.setAuditRemark(r.getAuditRemark());
            return row;
        }).collect(Collectors.toList());

        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            String fileName = URLEncoder.encode("上报数据_" + taskId, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), ExportRow.class).sheet("上报数据").doWrite(rows);
        } catch (IOException e) {
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }

    @Override
    public RecordAggregateResponse aggregate(Long taskId) {
        Map<String, Object> agg = recordMapper.selectAggregate(taskId);
        RecordAggregateResponse response = new RecordAggregateResponse();
        if (agg == null || agg.isEmpty()) {
            response.setTotal(0L);
            response.setDraft(0L);
            response.setSubmitted(0L);
            response.setApproved(0L);
            response.setRejected(0L);
            return response;
        }
        response.setTotal(toLong(agg.get("total")));
        response.setDraft(toLong(agg.get("draft")));
        response.setSubmitted(toLong(agg.get("submitted")));
        response.setApproved(toLong(agg.get("approved")));
        response.setRejected(toLong(agg.get("rejected")));
        return response;
    }

    private String statusLabel(Integer status) {
        if (status == null) return "";
        switch (status) {
            case 0: return "未提交";
            case 1: return "待审核";
            case 2: return "已通过";
            case 3: return "已驳回";
            default: return status.toString();
        }
    }

    private Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private List<WrRecordValue> buildValues(RecordSaveRequest req, Long recordId, Long templateId) {
        List<WrRecordValue> result = new ArrayList<>();

        if (req.getValues() != null) {
            for (RecordSaveRequest.CellValue cv : req.getValues()) {
                WrRecordValue v = newValue(recordId, templateId, cv.getItemId(), 1, cv.getValue());
                result.add(v);
            }
        }
        if (req.getRows() != null) {
            for (RecordSaveRequest.RowData row : req.getRows()) {
                if (row.getCells() == null) continue;
                for (RecordSaveRequest.CellValue cv : row.getCells()) {
                    WrRecordValue v = newValue(recordId, templateId, cv.getItemId(), row.getRowIndex(), cv.getValue());
                    result.add(v);
                }
            }
        }
        return result;
    }

    private WrRecordValue newValue(Long recordId, Long templateId, Long itemId, int rowIndex, String value) {
        WrRecordValue v = new WrRecordValue();
        v.setRecordId(recordId);
        v.setTemplateId(templateId);
        v.setItemId(itemId);
        v.setRowIndex(rowIndex);
        v.setCellValue(value);
        v.setCreateTime(LocalDateTime.now());
        v.setUpdateTime(LocalDateTime.now());
        return v;
    }

    @Data
    public static class ExportRow {
        @com.alibaba.excel.annotation.ExcelProperty("机构名称") private String orgName;
        @com.alibaba.excel.annotation.ExcelProperty("状态") private String status;
        @com.alibaba.excel.annotation.ExcelProperty("提交时间") private String submitTime;
        @com.alibaba.excel.annotation.ExcelProperty("审核结果") private String auditResult;
        @com.alibaba.excel.annotation.ExcelProperty("审核意见") private String auditRemark;
    }
}
