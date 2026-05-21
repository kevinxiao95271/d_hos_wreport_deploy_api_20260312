package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kxhospital.wreport.common.LoginUser;
import com.kxhospital.wreport.common.WrMessageConstants;
import com.kxhospital.wreport.entity.QcMessage;
import com.kxhospital.wreport.entity.QcMessageRead;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.mapper.QcMessageMapper;
import com.kxhospital.wreport.mapper.QcMessageReadMapper;
import com.kxhospital.wreport.mapper.SysUserMapper;
import com.kxhospital.wreport.service.WrTodoMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrTodoMessageServiceImpl implements WrTodoMessageService {

    private static final ZoneId APP_ZONE_ID = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DEADLINE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final QcMessageMapper qcMessageMapper;
    private final QcMessageReadMapper qcMessageReadMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public void sendTaskPublishTodos(WrTask task, List<Long> orgIds, LoginUser sender) {
        if (!isPublished(task)) {
            return;
        }
        List<Long> scope = orgIds == null ? Collections.emptyList() : orgIds;
        sendToOrgUsers(task, scope, sender);
    }

    @Override
    public void sendTaskTodosForAddedOrgs(WrTask task, List<Long> addedOrgIds, LoginUser sender) {
        if (!isPublished(task) || addedOrgIds == null || addedOrgIds.isEmpty()) {
            return;
        }
        sendToOrgUsers(task, addedOrgIds, sender);
    }

    @Override
    public void markTaskTodoHandled(Long taskId, Long userId) {
        if (taskId == null || userId == null) {
            return;
        }
        List<QcMessage> messages = qcMessageMapper.selectList(new LambdaQueryWrapper<QcMessage>()
                .eq(QcMessage::getDelFlag, "N")
                .eq(QcMessage::getMessageType, WrMessageConstants.MESSAGE_TYPE_TODO)
                .eq(QcMessage::getBusinessType, WrMessageConstants.BIZ_TYPE_WR_TASK_TODO)
                .eq(QcMessage::getBusinessId, taskId)
                .eq(QcMessage::getTargetType, WrMessageConstants.TARGET_TYPE_USER)
                .eq(QcMessage::getTargetId, userId));
        if (messages == null || messages.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(APP_ZONE_ID);
        int marked = 0;
        for (QcMessage msg : messages) {
            if (msg.getMessageId() == null) {
                continue;
            }
            QcMessageRead read = qcMessageReadMapper.selectOne(new LambdaQueryWrapper<QcMessageRead>()
                    .eq(QcMessageRead::getMessageId, msg.getMessageId())
                    .eq(QcMessageRead::getUserId, userId)
                    .last("limit 1"));
            if (read == null) {
                read = new QcMessageRead();
                read.setMessageId(msg.getMessageId());
                read.setUserId(userId);
                read.setReadTime(now);
                read.setProcessStatus(1);
                read.setProcessTime(now);
                qcMessageReadMapper.insert(read);
            } else {
                if (read.getProcessStatus() != null && read.getProcessStatus() == 1) {
                    continue;
                }
                if (read.getReadTime() == null) {
                    read.setReadTime(now);
                }
                read.setProcessStatus(1);
                read.setProcessTime(now);
                qcMessageReadMapper.updateById(read);
            }
            marked++;
        }
        if (marked > 0) {
            log.info("[wr-todo] taskId={} userId={} marked {} todo(s) handled", taskId, userId, marked);
        }
    }

    private boolean isPublished(WrTask task) {
        return task != null && task.getId() != null && task.getStatus() != null && task.getStatus() == 1;
    }

    private void sendToOrgUsers(WrTask task, List<Long> orgIds, LoginUser sender) {
        List<Map<String, Object>> receivers = resolveReceivers(orgIds);
        if (receivers.isEmpty()) {
            log.info("[wr-todo] no receivers for taskId={}", task.getId());
            return;
        }

        Set<Long> sentUserIds = new LinkedHashSet<>();
        String title = buildTitle(task);
        String content = buildContent(task);
        LocalDateTime now = LocalDateTime.now(APP_ZONE_ID);
        Long senderId = sender != null ? sender.getUserId() : null;
        String senderName = sender != null ? sender.getRealName() : null;

        int sent = 0;
        for (Map<String, Object> row : receivers) {
            Long userId = toLong(row.get("userId"));
            if (userId == null || !sentUserIds.add(userId)) {
                continue;
            }
            if (qcMessageMapper.countActiveUserTodo(
                    WrMessageConstants.BIZ_TYPE_WR_TASK_TODO, task.getId(), userId) > 0) {
                continue;
            }
            QcMessage msg = new QcMessage();
            msg.setTitle(title);
            msg.setContent(content);
            msg.setMessageType(WrMessageConstants.MESSAGE_TYPE_TODO);
            msg.setBusinessType(WrMessageConstants.BIZ_TYPE_WR_TASK_TODO);
            msg.setBusinessId(task.getId());
            msg.setTargetType(WrMessageConstants.TARGET_TYPE_USER);
            msg.setTargetId(userId);
            msg.setSendUserId(senderId);
            msg.setSendUserName(senderName);
            msg.setSendTime(now);
            msg.setCreateUser(senderId);
            msg.setCreateTime(now);
            msg.setUpdateUser(senderId);
            msg.setUpdateTime(now);
            msg.setDelFlag("N");
            qcMessageMapper.insert(msg);
            sent++;
        }
        log.info("[wr-todo] taskId={} sent {} todo(s)", task.getId(), sent);
    }

    private List<Map<String, Object>> resolveReceivers(List<Long> orgIds) {
        if (orgIds == null || orgIds.isEmpty()) {
            return sysUserMapper.listQcUserIdsByOrgIds(null);
        }
        return sysUserMapper.listQcUserIdsByOrgIds(orgIds);
    }

    private String buildTitle(WrTask task) {
        if ("daily_work".equals(task.getTaskType())) {
            return task.getStatQuarter() == null ? "年度日常工作填报待办" : "季度日常工作填报待办";
        }
        return "数据上报任务待办";
    }

    private String buildContent(WrTask task) {
        String name = task.getTaskName() != null ? task.getTaskName() : "上报任务";
        String deadlinePart = formatDeadlinePart(task.getDeadline());
        if ("daily_work".equals(task.getTaskType())) {
            if (task.getStatQuarter() == null) {
                return String.format("【%s】年度日常工作填报任务已发布，%s请点击「去处理」完成填报。", name, deadlinePart);
            }
            return String.format("【%s】第%d季度日常工作填报任务已发布，%s请点击「去处理」完成填报。",
                    name, task.getStatQuarter(), deadlinePart);
        }
        return String.format("【%s】上报任务已发布，%s请点击「去处理」完成填报。", name, deadlinePart);
    }

    private String formatDeadlinePart(LocalDateTime deadline) {
        if (deadline == null) {
            return "";
        }
        return "截止时间：" + deadline.format(DEADLINE_FMT) + "，";
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
