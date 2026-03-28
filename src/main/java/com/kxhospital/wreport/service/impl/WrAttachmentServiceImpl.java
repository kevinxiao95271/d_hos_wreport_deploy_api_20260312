package com.kxhospital.wreport.service.impl;

import com.kxhospital.wreport.common.BusinessException;
import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.WrAttachment;
import com.kxhospital.wreport.entity.WrRecord;
import com.kxhospital.wreport.entity.WrTask;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.mapper.WrAttachmentMapper;
import com.kxhospital.wreport.mapper.WrRecordMapper;
import com.kxhospital.wreport.mapper.WrTaskMapper;
import com.kxhospital.wreport.mapper.WrTemplateItemMapper;
import com.kxhospital.wreport.pojo.response.AttachmentVO;
import com.kxhospital.wreport.service.WrAttachmentService;
import com.kxhospital.wreport.service.WrTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrAttachmentServiceImpl implements WrAttachmentService {

    private final WrAttachmentMapper    attachmentMapper;
    private final WrRecordMapper        recordMapper;
    private final WrTaskMapper          taskMapper;
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

    // ========= 打包下载 =========

    @Override
    public void downloadZip(Long recordId, HttpServletResponse response) {
        // 1. 查记录
        WrRecord record = recordMapper.selectById(recordId);
        if (record == null) throw new BusinessException(404, "填报记录不存在");

        // 2. 拼 ZIP 文件名：{任务名}_{机构名}_{yyyyMMdd}.zip
        String taskName = "任务";
        if (record.getTaskId() != null) {
            WrTask task = taskMapper.selectById(record.getTaskId());
            if (task != null && task.getTaskName() != null) taskName = task.getTaskName();
        }
        String orgName = record.getOrgName() != null ? record.getOrgName() : "未知机构";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 过滤文件名中的非法字符
        String zipBaseName = (taskName + "_" + orgName + "_" + dateStr)
                .replaceAll("[\\\\/:*?\"<>|]", "_");
        String zipFileName = zipBaseName + ".zip";

        // 3. 查附件列表（含 itemName / headerPath）
        List<AttachmentVO> attachments = listByRecord(recordId);
        if (attachments.isEmpty()) throw new BusinessException(404, "该记录暂无附件");

        // 4. 设置响应头
        try {
            String encoded = URLEncoder.encode(zipFileName, "UTF-8").replace("+", "%20");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + zipFileName
                    + "\"; filename*=UTF-8''" + encoded);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
        } catch (Exception e) {
            throw new RuntimeException("设置响应头失败", e);
        }

        // 5. 流式写入 ZIP
        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream(),
                java.nio.charset.StandardCharsets.UTF_8)) {
            // 记录每个目录下的文件名，处理重名
            Map<String, Integer> nameCounter = new HashMap<>();

            for (AttachmentVO att : attachments) {
                if (att.getAttachPath() == null) continue;

                // 构造 ZIP 内路径：目录 + 文件名
                String dir = buildZipDir(att.getHeaderPath());
                String fileName = att.getAttachName() != null ? att.getAttachName() : "file";
                String entryPath = dir + deduplicateName(nameCounter, dir + fileName, fileName);

                InputStream in = null;
                try {
                    in = minioService.getObjectStream(minioProps.getBucketEvidence(), att.getAttachPath());
                    zos.putNextEntry(new ZipEntry(entryPath));
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        zos.write(buf, 0, len);
                    }
                    zos.closeEntry();
                } catch (Exception e) {
                    // 单个文件失败不中断整个 ZIP，写入占位说明文件
                    log.warn("[ZIP] 跳过文件 {} : {}", att.getAttachName(), e.getMessage());
                    try {
                        zos.putNextEntry(new ZipEntry(entryPath + ".error.txt"));
                        zos.write(("文件获取失败: " + e.getMessage()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        zos.closeEntry();
                    } catch (Exception ignored) {}
                } finally {
                    if (in != null) { try { in.close(); } catch (Exception ignored) {} }
                }
            }
            zos.finish();
        } catch (Exception e) {
            log.error("[ZIP] 打包失败 recordId={}: {}", recordId, e.getMessage(), e);
            throw new RuntimeException("打包下载失败: " + e.getMessage(), e);
        }
    }

    /** 将 headerPath 列表转为 ZIP 目录路径，末尾带 /。 */
    private String buildZipDir(List<String> headerPath) {
        if (headerPath == null || headerPath.isEmpty()) return "附件/";
        // 最后一级是文件所属节点名，作为目录名的最后一段
        return String.join("/", headerPath) + "/";
    }

    /**
     * 若同目录下同名文件已出现过，自动追加序号。
     * key = dir+fileName；返回去重后的文件名。
     */
    private String deduplicateName(Map<String, Integer> counter, String key, String fileName) {
        if (!counter.containsKey(key)) {
            counter.put(key, 1);
            return fileName;
        }
        int seq = counter.get(key) + 1;
        counter.put(key, seq);
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            return fileName.substring(0, dot) + "_" + seq + fileName.substring(dot);
        }
        return fileName + "_" + seq;
    }
}
