package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.*;
import com.kxhospital.wreport.mapper.*;
import com.kxhospital.wreport.pojo.request.*;
import com.kxhospital.wreport.cache.DwRegionCache;
import com.kxhospital.wreport.pojo.response.DwAdminOverviewVO;
import com.kxhospital.wreport.pojo.response.DwAttachmentVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO;
import com.kxhospital.wreport.pojo.response.DwRecordDetailVO.*;
import com.kxhospital.wreport.pojo.response.RegionNodeVO;
import com.kxhospital.wreport.pojo.response.TaskScopeOrgVO;
import com.kxhospital.wreport.service.DwRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class DwRecordServiceImpl implements DwRecordService {

    private final WrTaskMapper            taskMapper;
    private final WrRecordMapper          recordMapper;
    private final WrTaskOrgScopeMapper    taskOrgScopeMapper;
    private final DwMeetingMapper    meetingMapper;
    private final DwTrainingMapper   trainingMapper;
    private final DwGuidanceMapper   guidanceMapper;
    private final DwSurveyMapper     surveyMapper;
    private final DwFundingMapper    fundingMapper;
    private final DwBonusMapper      bonusMapper;
    private final DwAttachmentMapper attachmentMapper;
    private final MinioService       minioService;
    private final MinioProperties    minioProps;
    private final com.kxhospital.wreport.service.DwConfigService configService;
    private final DwRegionCache      regionCache;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ─────────────────────────────────────────────────────────────
    // 获取或初始化填报记录
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwRecordDetailVO getOrInitRecord(Long taskId, LoginUser user) {
        WrTask task = taskMapper.selectById(taskId);
        if (task == null || task.getDelFlag() == 1)
            throw new BusinessException(404, "任务不存在");
        if (!"daily_work".equals(task.getTaskType()))
            throw new BusinessException(400, "该任务不是日常工作模块任务");
        if (task.getStatus() != 1)
            throw new BusinessException(400, "任务尚未开放，无法填报");
        // 检查机构是否在任务范围内（scope 为空代表全机构可见）
        boolean inScope = taskMapper.isTaskInScope(taskId, user.getOrgId()) > 0;
        if (!inScope)
            throw new BusinessException(403, "您的机构不在该任务的填报范围内");

        WrRecord record = recordMapper.selectOne(new LambdaQueryWrapper<WrRecord>()
                .eq(WrRecord::getTaskId, taskId)
                .eq(WrRecord::getOrgId, user.getOrgId())
                .eq(WrRecord::getDelFlag, 0));

        if (record == null) {
            record = new WrRecord();
            record.setTaskId(taskId);
            record.setOrgId(user.getOrgId());
            record.setOrgName(user.getOrgName());
            record.setStatus(0);
            recordMapper.insert(record);
        }
        return buildDetail(record, task);
    }

    @Override
    public DwRecordDetailVO detail(Long recordId, LoginUser user) {
        WrRecord record = requireRecord(recordId);
        if (!user.isAdmin() && !record.getOrgId().equals(user.getOrgId()))
            throw new BusinessException(403, "无权查看该记录");
        WrTask task = taskMapper.selectById(record.getTaskId());
        return buildDetail(record, task);
    }

    // ─────────────────────────────────────────────────────────────
    // 质控会议
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwMeeting saveMeeting(DwMeetingRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        DwMeeting entity;
        if (req.getId() != null) {
            entity = meetingMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "会议记录不存在");
        } else {
            entity = new DwMeeting();
        }
        BeanUtils.copyProperties(req, entity, "id");
        if (req.getId() == null) meetingMapper.insert(entity);
        else meetingMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteMeeting(Long meetingId, LoginUser user) {
        DwMeeting entity = meetingMapper.selectById(meetingId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        meetingMapper.deleteById(meetingId);
        // 级联软删附件
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, meetingId)
                .eq(DwAttachment::getModuleType, "meeting"));
    }

    // ─────────────────────────────────────────────────────────────
    // 质控培训
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwTraining saveTraining(DwTrainingRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        DwTraining entity;
        if (req.getId() != null) {
            entity = trainingMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "培训记录不存在");
        } else {
            entity = new DwTraining();
        }
        BeanUtils.copyProperties(req, entity, "id");
        if (req.getId() == null) trainingMapper.insert(entity);
        else trainingMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteTraining(Long trainingId, LoginUser user) {
        DwTraining entity = trainingMapper.selectById(trainingId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        trainingMapper.deleteById(trainingId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, trainingId)
                .eq(DwAttachment::getModuleType, "training"));
    }

    // ─────────────────────────────────────────────────────────────
    // 质控指导
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwGuidance saveGuidance(DwGuidanceRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        int total = nvl(req.getCityCenterCount()) + nvl(req.getCountyCenterCount()) + nvl(req.getHospitalCount());
        if (total <= 0) throw new BusinessException(400, "市级中心数、县级中心数、医疗机构数不能全为 0");
        DwGuidance entity;
        if (req.getId() != null) {
            entity = guidanceMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "指导记录不存在");
        } else {
            entity = new DwGuidance();
        }
        BeanUtils.copyProperties(req, entity, "id");
        if (req.getId() == null) guidanceMapper.insert(entity);
        else guidanceMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteGuidance(Long guidanceId, LoginUser user) {
        DwGuidance entity = guidanceMapper.selectById(guidanceId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        guidanceMapper.deleteById(guidanceId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, guidanceId)
                .eq(DwAttachment::getModuleType, "guidance"));
    }

    // ─────────────────────────────────────────────────────────────
    // 质控调研
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwSurvey saveSurvey(DwSurveyRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        DwSurvey entity;
        if (req.getId() != null) {
            entity = surveyMapper.selectById(req.getId());
            if (entity == null) throw new BusinessException(404, "调研记录不存在");
        } else {
            entity = new DwSurvey();
        }
        BeanUtils.copyProperties(req, entity, "id");
        if (req.getId() == null) surveyMapper.insert(entity);
        else surveyMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteSurvey(Long surveyId, LoginUser user) {
        DwSurvey entity = surveyMapper.selectById(surveyId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        surveyMapper.deleteById(surveyId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, surveyId)
                .eq(DwAttachment::getModuleType, "survey"));
    }

    // ─────────────────────────────────────────────────────────────
    // 经费执行
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwFunding saveFunding(DwFundingRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        DwFunding existing = fundingMapper.findByRecord(req.getRecordId());
        DwFunding entity = existing != null ? existing : new DwFunding();
        BeanUtils.copyProperties(req, entity, "id");
        if (existing == null) fundingMapper.insert(entity);
        else fundingMapper.updateById(entity);
        return entity;
    }

    // ─────────────────────────────────────────────────────────────
    // 加分项
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwBonus saveBonus(DwBonusRequest req, LoginUser user) {
        requireEditableRecord(req.getRecordId(), user);
        DwBonus existing = bonusMapper.selectOne(new LambdaQueryWrapper<DwBonus>()
                .eq(DwBonus::getRecordId, req.getRecordId())
                .eq(DwBonus::getBonusType, req.getBonusType())
                .eq(DwBonus::getDelFlag, 0));
        DwBonus entity = existing != null ? existing : new DwBonus();
        BeanUtils.copyProperties(req, entity, "id");
        if (existing == null) bonusMapper.insert(entity);
        else bonusMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteBonus(Long bonusId, LoginUser user) {
        DwBonus entity = bonusMapper.selectById(bonusId);
        if (entity == null) return;
        requireEditableRecord(entity.getRecordId(), user);
        bonusMapper.deleteById(bonusId);
        attachmentMapper.delete(new LambdaQueryWrapper<DwAttachment>()
                .eq(DwAttachment::getSubRecordId, bonusId)
                .eq(DwAttachment::getModuleType, "bonus"));
    }

    // ─────────────────────────────────────────────────────────────
    // 附件上传 / 删除
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public DwAttachmentVO uploadAttachment(Long recordId, String moduleType,
                                           Long subRecordId, String slot,
                                           MultipartFile file, LoginUser user) {
        requireEditableRecord(recordId, user);
        String prefix = "dw/" + recordId + "/" + moduleType;
        String url = minioService.uploadEvidence(file, prefix);

        DwAttachment att = new DwAttachment();
        att.setRecordId(recordId);
        att.setModuleType(moduleType);
        att.setSubRecordId(subRecordId);
        att.setSlot(slot);
        att.setFileName(file.getOriginalFilename());
        att.setFileUrl(url);
        att.setFileSize(file.getSize());
        att.setFileMime(file.getContentType());
        attachmentMapper.insert(att);

        DwAttachmentVO vo = new DwAttachmentVO();
        BeanUtils.copyProperties(att, vo);
        return vo;
    }

    @Override
    @Transactional
    public void deleteAttachment(Long attachmentId, LoginUser user) {
        DwAttachment att = attachmentMapper.selectById(attachmentId);
        if (att == null) return;
        requireEditableRecord(att.getRecordId(), user);
        if (att.getFileUrl() != null)
            minioService.deleteByUrl(minioProps.getBucketEvidence(), att.getFileUrl());
        attachmentMapper.deleteById(attachmentId);
    }

    // ─────────────────────────────────────────────────────────────
    // 提交 / 审核
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void submit(Long recordId, LoginUser user) {
        WrRecord record = requireRecord(recordId);
        if (!record.getOrgId().equals(user.getOrgId()))
            throw new BusinessException(403, "无权操作");
        if (record.getStatus() != 0 && record.getStatus() != 3)
            throw new BusinessException(400, "当前状态不允许提交");
        record.setStatus(1);
        record.setSubmitUser(user.getUserId());
        record.setSubmitTime(java.time.LocalDateTime.now());
        recordMapper.updateById(record);
    }

    @Override
    @Transactional
    public void audit(Long recordId, Integer result, String remark, LoginUser user) {
        if (!user.isAdmin()) throw new BusinessException(403, "无权审核");
        WrRecord record = requireRecord(recordId);
        if (record.getStatus() != 1) throw new BusinessException(400, "记录尚未提交，无法审核");
        record.setStatus(result == 1 ? 2 : 3);
        record.setAuditUser(user.getUserId());
        record.setAuditTime(java.time.LocalDateTime.now());
        record.setAuditResult(result);
        record.setAuditRemark(remark);
        recordMapper.updateById(record);
    }

    // ─────────────────────────────────────────────────────────────
    // 内部：聚合详情构建
    // ─────────────────────────────────────────────────────────────

    private DwRecordDetailVO buildDetail(WrRecord record, WrTask task) {
        Long rid = record.getId();
        DwRecordDetailVO vo = new DwRecordDetailVO();
        vo.setRecordId(rid);
        vo.setTaskId(record.getTaskId());
        vo.setTaskName(task != null ? task.getTaskName() : null);
        vo.setOrgName(record.getOrgName());
        vo.setStatus(record.getStatus());
        vo.setAuditRemark(record.getAuditRemark());

        // 所有附件按 subRecordId + moduleType + slot 分组
        List<DwAttachment> allAtts = attachmentMapper.listByRecord(rid);
        Map<String, List<DwAttachment>> attMap = allAtts.stream()
                .collect(Collectors.groupingBy(a ->
                        a.getModuleType() + "|" + nvlId(a.getSubRecordId()) + "|" + a.getSlot()));

        // 所有扩展字段值，按 "moduleKey|subRecordId" 分组
        Map<String, Map<String, String>> extraMap = configService.loadAllValues(rid);

        // 质控会议
        vo.setMeetings(meetingMapper.listByRecord(rid).stream().map(m -> {
            DwMeetingVO mv = new DwMeetingVO();
            BeanUtils.copyProperties(m, mv);
            mv.setMeetingTime(m.getMeetingTime() != null ? m.getMeetingTime().format(DATE_FMT) : null);
            mv.setMinutes(toVOList(attMap.get("meeting|" + m.getId() + "|minutes")));
            mv.setPhotos(toVOList(attMap.get("meeting|" + m.getId() + "|photo")));
            mv.setSignins(toVOList(attMap.get("meeting|" + m.getId() + "|signin")));
            mv.setExtraValues(extraMap.getOrDefault("meeting|" + m.getId(), Collections.emptyMap()));
            return mv;
        }).collect(Collectors.toList()));

        // 质控培训
        vo.setTrainings(trainingMapper.listByRecord(rid).stream().map(t -> {
            DwTrainingVO tv = new DwTrainingVO();
            BeanUtils.copyProperties(t, tv);
            tv.setTrainingTime(t.getTrainingTime() != null ? t.getTrainingTime().format(DATE_FMT) : null);
            tv.setMaterials(toVOList(attMap.get("training|" + t.getId() + "|material")));
            tv.setPhotos(toVOList(attMap.get("training|" + t.getId() + "|photo")));
            tv.setExtraValues(extraMap.getOrDefault("training|" + t.getId(), Collections.emptyMap()));
            return tv;
        }).collect(Collectors.toList()));

        // 质控指导（含市/县质控中心名称反查及分组，数据来自内存缓存）
        Map<Integer, String> regionMap = buildRegionMap();
        vo.setGuidances(guidanceMapper.listByRecord(rid).stream().map(g -> {
            DwGuidanceVO gv = new DwGuidanceVO();
            BeanUtils.copyProperties(g, gv);
            gv.setGuidanceTime(g.getGuidanceTime() != null ? g.getGuidanceTime().format(DATE_FMT) : null);
            gv.setCityCenterNames(resolveRegionNames(g.getCityCenterIds(), regionMap));
            gv.setCountyCenterNames(resolveRegionNames(g.getCountyCenterIds(), regionMap));
            gv.setCountyCenterGroups(groupCountyCenters(g.getCountyCenterIds(), regionMap));
            gv.setEvidences(toVOList(attMap.get("guidance|" + g.getId() + "|evidence")));
            gv.setExtraValues(extraMap.getOrDefault("guidance|" + g.getId(), Collections.emptyMap()));
            return gv;
        }).collect(Collectors.toList()));

        // 质控调研
        vo.setSurveys(surveyMapper.listByRecord(rid).stream().map(s -> {
            DwSurveyVO sv = new DwSurveyVO();
            BeanUtils.copyProperties(s, sv);
            sv.setSurveyTime(s.getSurveyTime() != null ? s.getSurveyTime().format(DATE_FMT) : null);
            sv.setReports(toVOList(attMap.get("survey|" + s.getId() + "|report")));
            sv.setPhotos(toVOList(attMap.get("survey|" + s.getId() + "|photo")));
            sv.setExtraValues(extraMap.getOrDefault("survey|" + s.getId(), Collections.emptyMap()));
            return sv;
        }).collect(Collectors.toList()));

        // 纯上传模块（含 record 级扩展字段）
        vo.setAnnualWorkFiles(toVOList(attMap.get("annual_work|null|evidence")));
        vo.setAnnualWorkExtra(extraMap.getOrDefault("annual_work|null", Collections.emptyMap()));

        vo.setItConstructionFiles(toVOList(attMap.get("it_construction|null|evidence")));
        vo.setItConstructionExtra(extraMap.getOrDefault("it_construction|null", Collections.emptyMap()));

        Map<String, List<DwAttachmentVO>> workPlanMap = new LinkedHashMap<>();
        workPlanMap.put("plan",    toVOList(attMap.get("work_plan|null|plan")));
        workPlanMap.put("summary", toVOList(attMap.get("work_plan|null|summary")));
        vo.setWorkPlanFiles(workPlanMap);
        vo.setWorkPlanExtra(extraMap.getOrDefault("work_plan|null", Collections.emptyMap()));

        vo.setAdminResponseFiles(toVOList(attMap.get("admin_response|null|evidence")));
        vo.setAdminResponseExtra(extraMap.getOrDefault("admin_response|null", Collections.emptyMap()));

        Map<String, List<DwAttachmentVO>> activityMap = new LinkedHashMap<>();
        activityMap.put("pre_report",  toVOList(attMap.get("activity_report|null|pre_report")));
        activityMap.put("post_report", toVOList(attMap.get("activity_report|null|post_report")));
        vo.setActivityReportFiles(activityMap);
        vo.setActivityReportExtra(extraMap.getOrDefault("activity_report|null", Collections.emptyMap()));

        // 经费执行
        vo.setFunding(fundingMapper.findByRecord(rid));
        vo.setFundingExtra(extraMap.getOrDefault("funding|null", Collections.emptyMap()));

        // 加分项
        vo.setBonuses(bonusMapper.listByRecord(rid).stream().map(b -> {
            DwBonusVO bv = new DwBonusVO();
            BeanUtils.copyProperties(b, bv);
            bv.setPubDate(b.getPubDate() != null ? b.getPubDate().format(DATE_FMT) : null);
            bv.setCompDate(b.getCompDate() != null ? b.getCompDate().format(DATE_FMT) : null);
            bv.setEvidences(toVOList(attMap.get("bonus|" + b.getId() + "|evidence")));
            bv.setExtraValues(extraMap.getOrDefault("bonus|" + b.getId(), Collections.emptyMap()));
            return bv;
        }).collect(Collectors.toList()));

        return vo;
    }

    // ─────────────────────────────────────────────────────────────
    // 内部工具方法
    // ─────────────────────────────────────────────────────────────

    private WrRecord requireRecord(Long recordId) {
        WrRecord r = recordMapper.selectById(recordId);
        if (r == null || r.getDelFlag() == 1)
            throw new BusinessException(404, "填报记录不存在");
        return r;
    }

    private void requireEditableRecord(Long recordId, LoginUser user) {
        WrRecord r = requireRecord(recordId);
        if (!user.isAdmin() && !r.getOrgId().equals(user.getOrgId()))
            throw new BusinessException(403, "无权操作");
        if (!user.isAdmin() && r.getStatus() != 0 && r.getStatus() != 3)
            throw new BusinessException(400, "记录已提交，不可修改");
    }

    private List<DwAttachmentVO> toVOList(List<DwAttachment> list) {
        if (list == null) return Collections.emptyList();
        return list.stream().map(a -> {
            DwAttachmentVO v = new DwAttachmentVO();
            BeanUtils.copyProperties(a, v);
            return v;
        }).collect(Collectors.toList());
    }

    private int nvl(Integer v) { return v == null ? 0 : v; }
    private String nvlId(Long v) { return v == null ? "null" : String.valueOf(v); }

    /**
     * 从内存缓存构建 regionId → name 全量查找表。
     * 每次 buildDetail 调用一次，整个方法内复用，无 DB 访问。
     */
    private Map<Integer, String> buildRegionMap() {
        Map<Integer, String> map = new HashMap<>();
        com.kxhospital.wreport.pojo.response.GuidanceRegionsVO regions = regionCache.get();
        if (regions == null) return map;
        if (regions.getCityTree() != null) {
            for (RegionNodeVO node : regions.getCityTree()) {
                map.put(node.getId(), node.getName());
            }
        }
        if (regions.getCountyTree() != null) {
            for (RegionNodeVO city : regions.getCountyTree()) {
                map.put(city.getId(), city.getName());
                if (city.getChildren() != null) {
                    for (RegionNodeVO county : city.getChildren()) {
                        map.put(county.getId(), county.getName());
                    }
                }
            }
        }
        return map;
    }

    /**
     * 将县级中心 ID 列表按所属市分组。
     * ID 规则：区县 ID ÷ 100 = 所属市节点 ID（如 20101 ÷ 100 = 201 = 杭州市）。
     * 分组顺序依照原始 ID 首次出现的市顺序，组内顺序保持原始顺序。
     */
    private List<DwRecordDetailVO.CountyCenterGroupVO> groupCountyCenters(
            String jsonIds, Map<Integer, String> regionMap) {
        if (jsonIds == null || jsonIds.trim().isEmpty()) return Collections.emptyList();
        String trimmed = jsonIds.trim().replaceAll("[\\[\\]\\s]", "");
        if (trimmed.isEmpty() || "null".equals(trimmed)) return Collections.emptyList();

        // 按市节点 ID 有序分组（LinkedHashMap 保持首次出现顺序）
        Map<Integer, List<String>> grouped = new LinkedHashMap<>();
        for (String s : trimmed.split(",")) {
            if (s.isEmpty()) continue;
            try {
                int countyId = Integer.parseInt(s);
                int cityId   = countyId / 100;          // 20101 → 201
                String countyName = regionMap.getOrDefault(countyId, s);
                grouped.computeIfAbsent(cityId, k -> new ArrayList<>()).add(countyName);
            } catch (NumberFormatException ignored) { }
        }

        List<DwRecordDetailVO.CountyCenterGroupVO> result = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : grouped.entrySet()) {
            DwRecordDetailVO.CountyCenterGroupVO g = new DwRecordDetailVO.CountyCenterGroupVO();
            g.setCityName(regionMap.getOrDefault(entry.getKey(), String.valueOf(entry.getKey())));
            g.setCounties(entry.getValue());
            result.add(g);
        }
        return result;
    }

    /**
     * 将 JSON 整数数组字符串（如 "[101,103]"）解析并映射为名称列表。
     * 若某 ID 不在 regionMap 中则原样保留 ID 字符串，保证数据不丢失。
     */
    private List<String> resolveRegionNames(String jsonIds, Map<Integer, String> regionMap) {
        if (jsonIds == null || jsonIds.trim().isEmpty()) return Collections.emptyList();
        String trimmed = jsonIds.trim();
        if ("[]".equals(trimmed) || "null".equals(trimmed)) return Collections.emptyList();
        trimmed = trimmed.replaceAll("[\\[\\]\\s]", "");
        if (trimmed.isEmpty()) return Collections.emptyList();
        return Arrays.stream(trimmed.split(","))
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return regionMap.getOrDefault(Integer.parseInt(s), s);
                    } catch (NumberFormatException e) {
                        return s;
                    }
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────
    // 管理端汇总视图
    // ─────────────────────────────────────────────────────────────

    @Override
    public DwAdminOverviewVO adminOverview(Long taskId) {
        // 1. 获取任务范围内所有机构（含尚未填报的），并附带当前记录状态
        List<com.kxhospital.wreport.pojo.response.TaskScopeOrgVO> scopeList =
                taskOrgScopeMapper.selectScopeWithStatus(taskId);

        // 2. 批量获取已存在的 wr_record（用于取 recordId 和 orgId 映射）
        List<WrRecord> records = recordMapper.selectByTaskId(taskId);
        Map<Long, WrRecord> recordByOrg = records.stream()
                .collect(Collectors.toMap(WrRecord::getOrgId, r -> r, (a, b) -> a));

        // 3. 逐机构构建明细行，并汇总状态计数
        long notStarted = 0, draft = 0, submitted = 0, approved = 0, rejected = 0;
        List<DwAdminOverviewVO.OrgRow> orgRows = new ArrayList<>();

        for (com.kxhospital.wreport.pojo.response.TaskScopeOrgVO scope : scopeList) {
            DwAdminOverviewVO.OrgRow row = new DwAdminOverviewVO.OrgRow();
            row.setOrgId(scope.getOrgId());
            row.setOrgName(scope.getOrgName());

            WrRecord rec = recordByOrg.get(scope.getOrgId());
            if (rec == null) {
                row.setRecordId(null);
                row.setStatus(null);
                row.setStatusLabel("未开始");
                notStarted++;
            } else {
                row.setRecordId(rec.getId());
                row.setStatus(rec.getStatus());
                row.setStatusLabel(dwStatusLabel(rec.getStatus()));
                switch (rec.getStatus()) {
                    case 0: draft++;     break;
                    case 1: submitted++; break;
                    case 2: approved++;  break;
                    case 3: rejected++;  break;
                    default: break;
                }

                // 各模块数据量
                Long rid = rec.getId();
                row.setMeetingCount(meetingMapper.selectCount(
                        new LambdaQueryWrapper<DwMeeting>().eq(DwMeeting::getRecordId, rid)).intValue());
                row.setTrainingCount(trainingMapper.selectCount(
                        new LambdaQueryWrapper<DwTraining>().eq(DwTraining::getRecordId, rid)).intValue());
                row.setGuidanceCount(guidanceMapper.selectCount(
                        new LambdaQueryWrapper<DwGuidance>().eq(DwGuidance::getRecordId, rid)).intValue());
                row.setSurveyCount(surveyMapper.selectCount(
                        new LambdaQueryWrapper<DwSurvey>().eq(DwSurvey::getRecordId, rid)).intValue());
                row.setHasFunding(fundingMapper.selectCount(
                        new LambdaQueryWrapper<DwFunding>().eq(DwFunding::getRecordId, rid)) > 0);
                row.setBonusCount(bonusMapper.selectCount(
                        new LambdaQueryWrapper<DwBonus>().eq(DwBonus::getRecordId, rid)).intValue());
            }
            orgRows.add(row);
        }

        DwAdminOverviewVO vo = new DwAdminOverviewVO();
        vo.setTotal(scopeList.size());
        vo.setNotStarted(notStarted);
        vo.setDraft(draft);
        vo.setSubmitted(submitted);
        vo.setApproved(approved);
        vo.setRejected(rejected);
        vo.setOrgRows(orgRows);
        return vo;
    }

    private String dwStatusLabel(Integer status) {
        if (status == null) return "未开始";
        switch (status) {
            case 0: return "草稿";
            case 1: return "已提交";
            case 2: return "已通过";
            case 3: return "已驳回";
            default: return String.valueOf(status);
        }
    }
}
