package com.kxhospital.wreport.service.impl;

import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.WrAttachment;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.mapper.WrAttachmentMapper;
import com.kxhospital.wreport.mapper.WrRecordMapper;
import com.kxhospital.wreport.mapper.WrTemplateItemMapper;
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

    private final WrAttachmentMapper    attachmentMapper;
    private final WrRecordMapper        recordMapper;
    private final WrTemplateService     templateService;
    private final WrTemplateItemMapper  itemMapper;
    private final MinioService          minioService;
    private final MinioProperties       minioProps;

    @Override
    public AttachmentVO upload(Long recordId, Long itemId, MultipartFile file) {
        // ---- 上传校验（score 类模板专用，itemId 不为空时生效）----
        if (itemId != null) {
            WrTemplateItem item = itemMapper.selectById(itemId);
            if (item != null) {
                // 1. allowed_formats 校验：取文件扩展名与允许列表匹配（大小写不敏感）
                //    allowed_formats 为 null 或空串时不限制格式（error 4035）
                String formats = item.getAllowedFormats();
                if (formats != null && !formats.trim().isEmpty()) {
                    String originalName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename() : "";
                    int dotIdx = originalName.lastIndexOf('.');
                    String ext = dotIdx >= 0
                            ? originalName.substring(dotIdx + 1).toLowerCase()
                            : "";
                    boolean formatOk = java.util.Arrays.stream(formats.split(","))
                            .map(String::trim)
                            .map(String::toLowerCase)
                            .anyMatch(f -> f.equals(ext));
                    if (!formatOk) {
                        throw new BusinessException(4035,
                                "指标「" + item.getItemName() + "」仅支持上传 "
                                        + formats + " 格式，当前文件扩展名为「" + ext + "」");
                    }
                }
                // 2. max_attachments 校验：超出上限则拒绝（error 4034）
                if (item.getMaxAttachments() != null && item.getMaxAttachments() > 0) {
                    int current = attachmentMapper.countByRecordAndItem(recordId, itemId);
                    if (current >= item.getMaxAttachments()) {
                        throw new BusinessException(4034,
                                "指标「" + item.getItemName() + "」最多上传 " + item.getMaxAttachments()
                                        + " 个文件，当前已有 " + current + " 个");
                    }
                }
            }
        }

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

    /** 递归把树形 items 展平为一维列表，方便按 id 查找。 */
    private void flattenTree(List<WrTemplateItem> nodes, List<WrTemplateItem> result) {
        if (nodes == null) return;
        for (WrTemplateItem node : nodes) {
            result.add(node);
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                flattenTree(node.getChildren(), result);
            }
        }
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

        // templateService.items() 返回树形结构，需递归展平后建 Map
        List<WrTemplateItem> tree = templateService.items(record.getTemplateId());
        List<WrTemplateItem> flat = new java.util.ArrayList<>();
        flattenTree(tree, flat);
        return flat.stream()
                .filter(i -> neededIds.contains(i.getId()))
                .collect(Collectors.toMap(WrTemplateItem::getId, i -> i));
    }
}
