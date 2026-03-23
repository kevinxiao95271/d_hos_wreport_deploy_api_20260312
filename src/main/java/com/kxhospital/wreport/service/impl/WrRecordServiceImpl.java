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
import com.kxhospital.wreport.pojo.response.CrossViewVO;
import com.kxhospital.wreport.pojo.response.RecordAggregateResponse;
import com.kxhospital.wreport.pojo.response.RecordDetailVO;
import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.service.WrAttachmentService;
import com.kxhospital.wreport.service.WrRecordService;
import com.kxhospital.wreport.service.WrTemplateService;
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
import com.kxhospital.wreport.entity.WrDictItem;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.mapper.WrDictMapper;

@Service
@RequiredArgsConstructor
public class WrRecordServiceImpl implements WrRecordService {

    private final WrRecordMapper        recordMapper;
    private final WrRecordValueMapper   valueMapper;
    private final WrTaskMapper          taskMapper;
    private final WrTemplateItemMapper  itemMapper;
    private final WrAttachmentService   attachmentService;
    private final WrAttachmentMapper    attachmentMapper;
    private final WrTemplateService     templateService;
    private final WrTemplateMapper      templateMapper;
    private final WrDictMapper          dictMapper;
    private final WrTaskOrgScopeMapper  taskOrgScopeMapper;

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

        // ---- 总字数上限校验（form 类模板）----
        // max_total_chars > 0 时功能启用；= 0 / null 时跳过（不限制）。
        // 统计该 record 下所有 wr_record_value.cell_value 的字符总数（NULL 值不计入）。
        // 超出则返回错误码 4032，前端据此提示"字数超限"。
        WrTemplate tpl = templateMapper.selectById(record.getTemplateId());
        if (tpl != null && tpl.getMaxTotalChars() != null && tpl.getMaxTotalChars() > 0) {
            long totalChars = valueMapper.sumCharCount(record.getId());
            if (totalChars > tpl.getMaxTotalChars()) {
                throw new BusinessException(4032,
                        "填报总字数 " + totalChars + " 已超出模板限制 " + tpl.getMaxTotalChars() + " 字，请精简后重新提交");
            }
        }

        // ---- 最少附件数校验（score 类模板）----
        // 仅当模板类型为 "score" 时执行：遍历所有叶子指标，
        // 检查 min_attachments > 0 的指标是否已上传足够数量的文件。
        // 不足则返回错误码 4033，前端据此提示具体缺少哪个指标的文件。
        if (tpl != null && "score".equals(tpl.getTemplateType())) {
            List<WrTemplateItem> leafItems = itemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WrTemplateItem>()
                            .eq(WrTemplateItem::getTemplateId, tpl.getId())
                            .eq(WrTemplateItem::getIsLeaf, 1)
                            .eq(WrTemplateItem::getDelFlag, 0));
            for (WrTemplateItem leafItem : leafItems) {
                if (leafItem.getMinAttachments() != null && leafItem.getMinAttachments() > 0) {
                    int uploaded = attachmentMapper.countByRecordAndItem(record.getId(), leafItem.getId());
                    if (uploaded < leafItem.getMinAttachments()) {
                        throw new BusinessException(4033,
                                "指标「" + leafItem.getItemName() + "」至少需要上传 "
                                        + leafItem.getMinAttachments() + " 个文件，当前仅有 " + uploaded + " 个");
                    }
                }
            }
        }

        record.setStatus(1);
        record.setSubmitUser(user.getUserId());
        record.setSubmitTime(now);
        recordMapper.updateById(record);
    }

    /**
     * 查询单份填报当前已填字符总数，供前端实时显示进度条使用。
     * <p>返回内容：</p>
     * <ul>
     *   <li>currentChars   — 当前已填字符数（SUM LENGTH(cell_value)）</li>
     *   <li>maxTotalChars  — 模板设定上限（0 = 不限制）</li>
     *   <li>enabled        — maxTotalChars > 0 时为 true，前端据此决定是否显示进度条</li>
     * </ul>
     */
    @Override
    public Map<String, Object> charCount(Long recordId, LoginUser user) {
        WrRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("上报记录不存在");
        if (!record.getOrgId().equals(user.getOrgId())) throw new RuntimeException("无权查询");

        long current = valueMapper.sumCharCount(recordId);

        WrTemplate tpl = templateMapper.selectById(record.getTemplateId());
        int limit = (tpl != null && tpl.getMaxTotalChars() != null) ? tpl.getMaxTotalChars() : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currentChars",  current);
        result.put("maxTotalChars", limit);
        // enabled = true 时前端显示进度条；false 时隐藏即可
        result.put("enabled",       limit > 0);
        return result;
    }

    /**
     * 评分汇总（score 类模板专用）。
     * 遍历模板所有叶子指标，统计每个指标已上传的文件数，
     * 与 min_attachments 比较判断是否"达标"，达标则计入 score_value。
     */
    @Override
    public Map<String, Object> scoreDetail(Long recordId, LoginUser user) {
        WrRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new RuntimeException("上报记录不存在");
        // 管理员不受限；机构用户只能查自己机构的记录
        if (!user.isAdmin() && (user.getOrgId() == null || !record.getOrgId().equals(user.getOrgId()))) {
            throw new BusinessException(403, "无权查询该记录的评分汇总");
        }

        WrTemplate tpl = templateMapper.selectById(record.getTemplateId());
        if (tpl == null || !"score".equals(tpl.getTemplateType())) {
            throw new RuntimeException("该记录对应的模板不是评分细则类型");
        }

        // 查该模板的所有叶子指标
        List<WrTemplateItem> leafItems = itemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WrTemplateItem>()
                        .eq(WrTemplateItem::getTemplateId, tpl.getId())
                        .eq(WrTemplateItem::getIsLeaf, 1)
                        .eq(WrTemplateItem::getDelFlag, 0)
                        .orderByAsc(WrTemplateItem::getSortNum));

        java.math.BigDecimal totalScore = java.math.BigDecimal.ZERO;
        java.math.BigDecimal maxScore   = java.math.BigDecimal.ZERO;
        List<Map<String, Object>> itemDetails = new ArrayList<>();

        for (WrTemplateItem leaf : leafItems) {
            int uploaded = attachmentMapper.countByRecordAndItem(recordId, leaf.getId());
            int minReq   = leaf.getMinAttachments() != null ? leaf.getMinAttachments() : 0;
            // 达标条件：minAttachments=0 表示不要求（默认达标）；> 0 时须满足数量
            boolean reached = (minReq == 0) || (uploaded >= minReq);
            java.math.BigDecimal score = leaf.getScoreValue() != null
                    ? leaf.getScoreValue() : java.math.BigDecimal.ZERO;

            maxScore = maxScore.add(score);
            if (reached) totalScore = totalScore.add(score);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemId",         leaf.getId());
            row.put("itemName",       leaf.getItemName());
            row.put("minAttachments", minReq);
            row.put("maxAttachments", leaf.getMaxAttachments() != null ? leaf.getMaxAttachments() : 0);
            row.put("uploaded",       uploaded);
            row.put("reached",        reached);
            row.put("scoreValue",     score);
            itemDetails.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalScore", totalScore);
        result.put("maxScore",   maxScore);
        result.put("items",      itemDetails);
        return result;
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
        List<WrRecordValue>  values      = valueMapper.selectByRecordId(recordId);
        List<AttachmentVO>   attachments = attachmentService.listByRecord(recordId);

        // 从 values 中提取本记录实际涉及的 itemId 集合
        Set<Long> usedItemIds = values.stream()
                .map(WrRecordValue::getItemId)
                .collect(Collectors.toSet());

        // 获取模板全量列定义，只保留：
        //   1. 本记录 values 中出现过的叶子列（isLeaf=1）
        //   2. 这些叶子列的所有祖先节点（isLeaf=0），用于前端构建多级表头
        List<WrTemplateItem> allItems = templateService.items(record.getTemplateId());
        Set<Long> ancestorIds = new HashSet<>();
        for (WrTemplateItem it : allItems) {
            if (it.getIsLeaf() == 1 && usedItemIds.contains(it.getId())) {
                // 向上追溯父节点
                Long pid = it.getParentId();
                while (pid != null) {
                    ancestorIds.add(pid);
                    final Long fpid = pid;
                    pid = allItems.stream()
                            .filter(x -> x.getId().equals(fpid))
                            .findFirst()
                            .map(WrTemplateItem::getParentId)
                            .orElse(null);
                }
            }
        }
        List<WrTemplateItem> filteredItems = allItems.stream()
                .filter(it -> usedItemIds.contains(it.getId()) || ancestorIds.contains(it.getId()))
                .collect(Collectors.toList());

        RecordDetailVO vo = new RecordDetailVO();
        vo.setRecord(record);
        vo.setValues(values);
        vo.setAttachments(attachments);
        vo.setStatusLabel(statusLabel(record.getStatus()));
        vo.setItems(filteredItems);
        List<WrTemplateRow> rows = templateService.listRows(record.getTemplateId());
        vo.setRows(rows);
        // 两种模板都返回 filteredItems：
        //   标准模板 → 所有叶子列 + 祖先节点，供前端构建多级表头
        //   矩阵模板 → 仅本机构那1列（含 itemId），前端保存时需要；列名不展示即可
        vo.setItems(filteredItems);
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
        // total = 该任务分配的机构总数（无论是否已开始填报）
        long assignedTotal = taskOrgScopeMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<WrTaskOrgScope>()
                        .eq(WrTaskOrgScope::getTaskId, taskId));

        Map<String, Object> agg = recordMapper.selectAggregate(taskId);
        RecordAggregateResponse response = new RecordAggregateResponse();
        response.setTotal(assignedTotal);
        if (agg == null || agg.isEmpty()) {
            response.setDraft(0L);
            response.setSubmitted(0L);
            response.setApproved(0L);
            response.setRejected(0L);
            return response;
        }
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

    // ──────────────────────────────────────────────────────────────────────────
    // crossView：跨机构横向对比视图
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public CrossViewVO crossView(Long taskId, List<Long> itemIds, List<Integer> rowIndexes) {
        WrTask task = taskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        Long templateId = task.getTemplateId();

        // 全量模板行/列定义
        List<WrTemplateItem> allItems = templateService.items(templateId);
        List<WrTemplateRow>  allRows  = templateService.listRows(templateId);
        boolean isMatrix = !allRows.isEmpty();

        // 查该任务所有 record
        List<WrRecord> records = recordMapper.selectByTaskId(taskId);
        if (records.isEmpty()) {
            CrossViewVO vo = new CrossViewVO();
            vo.setTemplateType(isMatrix ? "matrix" : "standard");
            return vo;
        }

        // 一次批量拉取所有 record 的值
        List<Long> recordIds = records.stream().map(WrRecord::getId).collect(Collectors.toList());
        List<WrRecordValue> allValues = valueMapper.selectByRecordIds(recordIds);

        // recordId → List<Value>
        Map<Long, List<WrRecordValue>> valuesByRecord = allValues.stream()
                .collect(Collectors.groupingBy(WrRecordValue::getRecordId));

        // 构建字典翻译 map：dictCode → { itemValue → itemLabel }
        Map<String, Map<String, String>> dictLabelMap = buildDictLabelMap(allItems);

        CrossViewVO vo = new CrossViewVO();
        vo.setTemplateType(isMatrix ? "matrix" : "standard");

        if (!isMatrix) {
            // ── 标准模板（附件2）────────────────────────────────────────────────
            // 过滤展示列：itemIds 为空时取全部叶子列
            List<WrTemplateItem> showItems = allItems.stream()
                    .filter(i -> i.getIsLeaf() == 1)
                    .filter(i -> itemIds == null || itemIds.isEmpty() || itemIds.contains(i.getId()))
                    .collect(Collectors.toList());
            vo.setItems(showItems);
            Set<Long> showItemIdSet = showItems.stream().map(WrTemplateItem::getId).collect(Collectors.toSet());

            List<CrossViewVO.OrgRow> orgRows = new ArrayList<>();
            for (WrRecord rec : records) {
                CrossViewVO.OrgRow row = new CrossViewVO.OrgRow();
                row.setOrgId(rec.getOrgId());
                row.setOrgName(rec.getOrgName());
                row.setRecordId(rec.getId());
                row.setStatus(rec.getStatus());
                row.setStatusLabel(statusLabel(rec.getStatus()));

                List<WrRecordValue> recValues = valuesByRecord.getOrDefault(rec.getId(), Collections.emptyList());
                List<CrossViewVO.Cell> cells = new ArrayList<>();
                for (WrRecordValue rv : recValues) {
                    if (!showItemIdSet.contains(rv.getItemId())) continue;
                    CrossViewVO.Cell cell = new CrossViewVO.Cell();
                    cell.setItemId(rv.getItemId());
                    cell.setCellValue(rv.getCellValue());
                    cell.setCellLabel(translateCell(rv.getItemId(), rv.getCellValue(), allItems, dictLabelMap));
                    cells.add(cell);
                }
                row.setCells(cells);
                orgRows.add(row);
            }
            vo.setOrgRows(orgRows);

        } else {
            // ── 矩阵模板（附件3）────────────────────────────────────────────────
            // 分离 checkbox 列 和 number 列
            List<WrTemplateItem> numberItems = allItems.stream()
                    .filter(i -> i.getIsLeaf() == 1 && !"checkbox".equals(i.getValueType()))
                    .collect(Collectors.toList());
            Set<Long> numberItemIds = numberItems.stream().map(WrTemplateItem::getId).collect(Collectors.toSet());

            // rowIndexes 过滤：非空时只保留指定行，同时自动补入其祖先行保证树结构完整
            List<WrTemplateRow> showRows;
            if (rowIndexes != null && !rowIndexes.isEmpty()) {
                Set<Integer> selected = new HashSet<>(rowIndexes);
                // 向上补祖先行，保证前端能正确构建联动树
                Map<Integer, WrTemplateRow> rowMap = allRows.stream()
                        .collect(Collectors.toMap(WrTemplateRow::getRowIndex, r -> r));
                Set<Integer> toShow = new HashSet<>(selected);
                for (Integer ri : selected) {
                    WrTemplateRow cur = rowMap.get(ri);
                    while (cur != null && cur.getParentRowIndex() != null) {
                        toShow.add(cur.getParentRowIndex());
                        cur = rowMap.get(cur.getParentRowIndex());
                    }
                }
                showRows = allRows.stream()
                        .filter(r -> toShow.contains(r.getRowIndex()))
                        .collect(Collectors.toList());
            } else {
                showRows = allRows;
            }
            vo.setRows(showRows);
            vo.setNumberItems(numberItems);

            // orgCols：每个机构一列
            List<CrossViewVO.OrgCol> orgCols = new ArrayList<>();
            for (WrRecord rec : records) {
                CrossViewVO.OrgCol col = new CrossViewVO.OrgCol();
                col.setOrgId(rec.getOrgId());
                col.setOrgName(rec.getOrgName());
                col.setRecordId(rec.getId());
                col.setStatus(rec.getStatus());
                col.setStatusLabel(statusLabel(rec.getStatus()));
                orgCols.add(col);
            }
            vo.setOrgCols(orgCols);

            // matrixValues / numberValues
            List<CrossViewVO.MatrixCell> matrixValues = new ArrayList<>();
            List<CrossViewVO.NumberCell> numberValues  = new ArrayList<>();
            for (WrRecord rec : records) {
                List<WrRecordValue> recValues = valuesByRecord.getOrDefault(rec.getId(), Collections.emptyList());
                for (WrRecordValue rv : recValues) {
                    if (numberItemIds.contains(rv.getItemId())) {
                        CrossViewVO.NumberCell nc = new CrossViewVO.NumberCell();
                        nc.setOrgId(rec.getOrgId());
                        nc.setItemId(rv.getItemId());
                        nc.setCellValue(rv.getCellValue());
                        numberValues.add(nc);
                    } else {
                        CrossViewVO.MatrixCell mc = new CrossViewVO.MatrixCell();
                        mc.setOrgId(rec.getOrgId());
                        mc.setRowIndex(rv.getRowIndex());
                        mc.setCellValue(rv.getCellValue());
                        matrixValues.add(mc);
                    }
                }
            }
            // matrixValues 也按 showRows 过滤
            if (rowIndexes != null && !rowIndexes.isEmpty()) {
                Set<Integer> showRowIndexSet = showRows.stream()
                        .map(WrTemplateRow::getRowIndex).collect(Collectors.toSet());
                matrixValues = matrixValues.stream()
                        .filter(mc -> showRowIndexSet.contains(mc.getRowIndex()))
                        .collect(Collectors.toList());
            }
            vo.setMatrixValues(matrixValues);
            vo.setNumberValues(numberValues);
        }
        return vo;
    }

    /** 构建字典翻译 map：dictCode → { itemValue → itemLabel } */
    private Map<String, Map<String, String>> buildDictLabelMap(List<WrTemplateItem> items) {
        Set<String> dictCodes = items.stream()
                .filter(i -> i.getDictCode() != null)
                .map(WrTemplateItem::getDictCode)
                .collect(Collectors.toSet());
        Map<String, Map<String, String>> result = new HashMap<>();
        for (String code : dictCodes) {
            List<WrDictItem> dictItems = dictMapper.selectItemsByCode(code);
            Map<String, String> valToLabel = new HashMap<>();
            dictItems.forEach(di -> valToLabel.put(di.getItemValue(), di.getItemLabel()));
            result.put(code, valToLabel);
        }
        return result;
    }

    /** 翻译单个格的值：有字典则返回 label，否则返回原值 */
    private String translateCell(Long itemId, String cellValue,
                                 List<WrTemplateItem> allItems,
                                 Map<String, Map<String, String>> dictLabelMap) {
        if (cellValue == null) return null;
        return allItems.stream()
                .filter(i -> i.getId().equals(itemId) && i.getDictCode() != null)
                .findFirst()
                .map(i -> dictLabelMap.getOrDefault(i.getDictCode(), Collections.emptyMap())
                        .getOrDefault(cellValue, cellValue))
                .orElse(cellValue);
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
