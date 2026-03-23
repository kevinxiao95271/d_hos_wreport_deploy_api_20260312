package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.WrTemplate;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.entity.WrTemplateRow;
import com.kxhospital.wreport.mapper.WrTemplateItemMapper;
import com.kxhospital.wreport.mapper.WrTemplateMapper;
import com.kxhospital.wreport.mapper.WrTemplateRowMapper;
import com.kxhospital.wreport.pojo.request.TemplateItemRequest;
import com.kxhospital.wreport.pojo.response.TemplateDetailVO;
import com.kxhospital.wreport.service.WrTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrTemplateServiceImpl implements WrTemplateService {

    private final WrTemplateMapper     templateMapper;
    private final WrTemplateItemMapper itemMapper;
    private final WrTemplateRowMapper  rowMapper;
    private final MinioService         minioService;
    private final MinioProperties      minioProps;

    @Override
    public IPage<WrTemplate> page(Page<WrTemplate> page, String templateName, Integer status) {
        return templateMapper.selectPage(page, templateName, status);
    }

    @Override
    public WrTemplate detail(Long id) {
        return templateMapper.selectById(id);
    }

    @Override
    public TemplateDetailVO detailFull(Long id) {
        TemplateDetailVO vo = new TemplateDetailVO();
        vo.setTemplate(templateMapper.selectById(id));
        // items 与 GET /wr/template/items 保持一致：已建树、已填 headerPath、按 sort_num 排序
        List<WrTemplateItem> flat = itemMapper.selectByTemplateId(id);
        fillHeaderPath(flat);
        vo.setItems(buildTree(flat));
        vo.setRows(rowMapper.selectByTemplateId(id));
        return vo;
    }

    @Override
    public List<WrTemplateItem> items(Long templateId) {
        List<WrTemplateItem> items = itemMapper.selectByTemplateId(templateId);
        fillHeaderPath(items);
        return buildTree(items);
    }

    /**
     * 将平铺列表按 parentId 关系组装成树，返回根节点列表。
     * <p>Mapper 已按 sort_num ASC 排序，建树后兄弟节点顺序忠于模板设计顺序。</p>
     */
    private List<WrTemplateItem> buildTree(List<WrTemplateItem> items) {
        if (items == null || items.isEmpty()) return items;
        Map<Long, WrTemplateItem> byId = items.stream()
                .collect(Collectors.toMap(WrTemplateItem::getId, i -> i));
        List<WrTemplateItem> roots = new ArrayList<>();
        for (WrTemplateItem item : items) {
            if (item.getParentId() == null) {
                roots.add(item);
            } else {
                WrTemplateItem parent = byId.get(item.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new ArrayList<>());
                    parent.getChildren().add(item);
                } else {
                    // 父节点找不到（数据异常），当根节点兜底
                    roots.add(item);
                }
            }
        }
        return roots;
    }

    /**
     * 为列表中每个节点计算并填充 headerPath（从根到本节点的名称列表）。
     * 例如三级结构：["2024年", "学术会议", "线上次数"]
     * 前端填报页面可用 headerPath.join(" / ") 作为字段标签，避免重名歧义。
     */
    private void fillHeaderPath(List<WrTemplateItem> items) {
        if (items == null || items.isEmpty()) return;
        Map<Long, WrTemplateItem> byId = items.stream()
                .collect(Collectors.toMap(WrTemplateItem::getId, i -> i));
        for (WrTemplateItem item : items) {
            List<String> path = new ArrayList<>();
            WrTemplateItem cur = item;
            while (cur != null) {
                path.add(0, cur.getItemName());
                cur = cur.getParentId() != null ? byId.get(cur.getParentId()) : null;
            }
            item.setHeaderPath(path);
        }
    }

    @Override
    @Transactional
    public Long add(WrTemplate template, List<TemplateItemRequest> requests) {
        template.setStatus(0);
        templateMapper.insert(template);
        if (requests != null && !requests.isEmpty()) {
            insertItemsWithClientIdMapping(template.getId(), requests);
        }
        return template.getId();
    }

    @Override
    public void update(WrTemplate template) {
        templateMapper.updateById(template);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        LambdaUpdateWrapper<WrTemplate> w = new LambdaUpdateWrapper<>();
        w.eq(WrTemplate::getId, id).set(WrTemplate::getStatus, status);
        templateMapper.update(null, w);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        templateMapper.deleteById(id);
        // 同时逻辑删除 items
        LambdaUpdateWrapper<WrTemplateItem> w = new LambdaUpdateWrapper<>();
        w.eq(WrTemplateItem::getTemplateId, id).set(WrTemplateItem::getDelFlag, 1);
        itemMapper.update(null, w);
    }

    @Override
    @Transactional
    public void replaceItems(Long templateId, List<TemplateItemRequest> requests) {
        if (requests == null) requests = Collections.emptyList();
        validateItemRequests(requests);
        itemMapper.physicalDeleteByTemplateId(templateId);
        if (!requests.isEmpty()) {
            insertItemsWithClientIdMapping(templateId, requests);
        }
    }

    /**
     * 写前校验：
     *   ① id 不重复且不为空
     *   ② 所有 parentId 引用的 id 在本批次存在或为 null
     *   ③ 无环（防止 A→B→A 之类的循环引用）
     */
    private void validateItemRequests(List<TemplateItemRequest> requests) {
        Set<String> ids = new HashSet<>();
        for (TemplateItemRequest r : requests) {
            if (r.getId() == null || r.getId().trim().isEmpty()) {
                throw new RuntimeException("每个节点必须带有 id 字段（可为已有 DB id 或前端临时 id）");
            }
            if (!ids.add(r.getId().trim())) {
                throw new RuntimeException("id 重复: " + r.getId());
            }
        }
        for (TemplateItemRequest r : requests) {
            if (r.getParentId() != null && !ids.contains(r.getParentId().trim())) {
                throw new RuntimeException("parentId 引用了不存在的 id: [" + r.getParentId()
                        + "] (节点: " + r.getItemName() + ")");
            }
        }
        // 环检测
        Map<String, String> parentMap = new HashMap<>();
        for (TemplateItemRequest r : requests) {
            parentMap.put(r.getId().trim(),
                    r.getParentId() != null ? r.getParentId().trim() : null);
        }
        for (String startId : ids) {
            Set<String> visited = new HashSet<>();
            String cur = startId;
            while (cur != null) {
                if (!visited.add(cur)) {
                    throw new RuntimeException("检测到环形引用，节点 id: " + cur);
                }
                cur = parentMap.get(cur);
            }
        }
    }

    /**
     * 两阶段 insert：
     *   阶段1：为每个节点确定真实 DB id（纯数字 clientId → 复用；非纯数字 → 生成新 Snowflake）
     *   阶段2：按阶段1的映射写入，parentId 通过映射表翻译为真实 Long id
     */
    private void insertItemsWithClientIdMapping(Long templateId, List<TemplateItemRequest> requests) {
        // 阶段1：clientId → realId
        Map<String, Long> clientToReal = new LinkedHashMap<>();
        for (TemplateItemRequest r : requests) {
            String cid = r.getId().trim();
            Long realId;
            try {
                realId = Long.parseLong(cid);
            } catch (NumberFormatException e) {
                realId = IdWorker.getId();
            }
            clientToReal.put(cid, realId);
        }
        // 阶段2：insert
        for (TemplateItemRequest r : requests) {
            WrTemplateItem item = new WrTemplateItem();
            BeanUtils.copyProperties(r, item);  // 复制 itemName/headerRow/colIndex 等同类型字段
            item.setId(clientToReal.get(r.getId().trim()));
            item.setTemplateId(templateId);
            item.setDelFlag(0);
            item.setParentId(r.getParentId() != null
                    ? clientToReal.get(r.getParentId().trim())
                    : null);
            // dictCode 已由 BeanUtils 复制（String→String），无需额外处理
            itemMapper.insert(item);
        }
    }

    @Override
    public void updateItemDict(Long itemId, String dictCode) {
        if (itemMapper.selectById(itemId) == null) throw new RuntimeException("表头项不存在: " + itemId);
        // 空串统一视为 null（解绑）
        String value = (dictCode != null && !dictCode.trim().isEmpty()) ? dictCode.trim() : null;
        // 必须用 LambdaUpdateWrapper 才能显式写 NULL，updateById 默认跳过 null 字段
        LambdaUpdateWrapper<WrTemplateItem> w = new LambdaUpdateWrapper<>();
        w.eq(WrTemplateItem::getId, itemId).set(WrTemplateItem::getDictCode, value);
        itemMapper.update(null, w);
    }

    @Override
    public List<WrTemplate> listActive() {
        return templateMapper.selectActiveList();
    }

    @Override
    public List<WrTemplateRow> listRows(Long templateId) {
        return rowMapper.selectByTemplateId(templateId);
    }

    @Override
    @Transactional
    public void replaceRows(Long templateId, List<WrTemplateRow> rows) {
        rowMapper.deleteByTemplateId(templateId);
        if (rows == null || rows.isEmpty()) return;
        int sortNum = 0;
        for (WrTemplateRow row : rows) {
            row.setId(null);
            row.setTemplateId(templateId);
            row.setDelFlag(0);
            if (row.getSortNum() == null) row.setSortNum(sortNum++);
            rowMapper.insert(row);
        }
    }

    @Override
    public String uploadFormatTemplate(Long itemId, MultipartFile file) {
        WrTemplateItem item = itemMapper.selectById(itemId);
        if (item == null) throw new RuntimeException("表头项不存在: " + itemId);

        // 删除旧文件
        if (item.getFormatTemplateUrl() != null) {
            minioService.deleteByUrl(minioProps.getBucketTemplate(), item.getFormatTemplateUrl());
        }

        String url = minioService.uploadTemplate(file, "templates");
        item.setFormatTemplateUrl(url);
        item.setFormatTemplateName(file.getOriginalFilename());
        itemMapper.updateById(item);
        return url;
    }

    @Override
    public void deleteFormatTemplate(Long itemId) {
        WrTemplateItem item = itemMapper.selectById(itemId);
        if (item == null) return;
        if (item.getFormatTemplateUrl() != null) {
            minioService.deleteByUrl(minioProps.getBucketTemplate(), item.getFormatTemplateUrl());
        }
        item.setFormatTemplateUrl(null);
        item.setFormatTemplateName(null);
        itemMapper.updateById(item);
    }
}
