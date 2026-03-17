package com.kxhospital.wreport.service.impl;

import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.WrAttachment;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.mapper.WrAttachmentMapper;
import com.kxhospital.wreport.mapper.WrRecordMapper;
import com.kxhospital.wreport.pojo.response.AttachmentVO;
import com.kxhospital.wreport.service.WrAttachmentService;
import com.kxhospital.wreport.service.WrTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrAttachmentServiceImpl implements WrAttachmentService {

    private final WrAttachmentMapper attachmentMapper;
    private final WrRecordMapper     recordMapper;
    private final WrTemplateService  templateService;
    private final MinioService       minioService;
    private final MinioProperties    minioProps;

    @Override
    public AttachmentVO upload(Long recordId, Long itemId, MultipartFile file) {
        String prefix = "record/" + recordId;
        String url    = minioService.uploadEvidence(file, prefix);

        WrAttachment attach = new WrAttachment();
        attach.setRecordId(recordId);
        attach.setItemId(itemId);
        attach.setAttachName(file.getOriginalFilename());
        attach.setAttachPath(url);
        attach.setAttachSize(file.getSize());
        attach.setAttachType(file.getContentType());
        attachmentMapper.insert(attach);

        AttachmentVO vo = new AttachmentVO();
        BeanUtils.copyProperties(attach, vo);
        return vo;
    }

    @Override
    public void delete(Long attachmentId) {
        WrAttachment attach = attachmentMapper.selectById(attachmentId);
        if (attach == null) return;
        if (attach.getAttachPath() != null) {
            minioService.deleteByUrl(minioProps.getBucketEvidence(), attach.getAttachPath());
        }
        attachmentMapper.deleteById(attachmentId);
    }

    @Override
    public List<AttachmentVO> listByRecord(Long recordId) {
        List<WrAttachment> attachments = attachmentMapper.selectByRecordId(recordId);
        if (attachments.isEmpty()) return Collections.emptyList();

        // 构建 itemId → WrTemplateItem 映射（含 headerPath）
        Map<Long, WrTemplateItem> itemMap = buildItemMap(recordId, attachments);

        return attachments.stream().map(a -> {
            AttachmentVO vo = new AttachmentVO();
            BeanUtils.copyProperties(a, vo);
            if (a.getItemId() == null) {
                vo.setItemName("整体附件");
                vo.setHeaderPath(Collections.singletonList("整体附件"));
            } else {
                WrTemplateItem item = itemMap.get(a.getItemId());
                if (item != null) {
                    vo.setItemName(item.getItemName());
                    vo.setHeaderPath(item.getHeaderPath() != null
                            ? item.getHeaderPath()
                            : Collections.singletonList(item.getItemName()));
                } else {
                    // item 已被删除或 ID 失效，降级展示
                    vo.setItemName("未知节点");
                    vo.setHeaderPath(Collections.singletonList("未知节点"));
                }
            }
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 根据 recordId 找到其模板，批量获取带 headerPath 的 items，
     * 返回 itemId → WrTemplateItem 的查找 Map。
     */
    private Map<Long, WrTemplateItem> buildItemMap(Long recordId, List<WrAttachment> attachments) {
        // 只处理有 itemId 的附件
        Set<Long> neededIds = attachments.stream()
                .map(WrAttachment::getItemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (neededIds.isEmpty()) return Collections.emptyMap();

        WrRecord record = recordMapper.selectById(recordId);
        if (record == null || record.getTemplateId() == null) return Collections.emptyMap();

        // templateService.items() 内部已调用 fillHeaderPath，直接可用
        List<WrTemplateItem> items = templateService.items(record.getTemplateId());
        return items.stream()
                .filter(i -> neededIds.contains(i.getId()))
                .collect(Collectors.toMap(WrTemplateItem::getId, i -> i));
    }
}
