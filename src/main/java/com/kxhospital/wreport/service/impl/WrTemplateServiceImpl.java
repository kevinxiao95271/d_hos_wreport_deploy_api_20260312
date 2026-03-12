package com.kxhospital.wreport.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
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
import com.kxhospital.wreport.pojo.response.TemplateDetailVO;
import com.kxhospital.wreport.service.WrTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

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
        vo.setItems(itemMapper.selectByTemplateId(id));
        vo.setRows(rowMapper.selectByTemplateId(id));
        return vo;
    }

    @Override
    public List<WrTemplateItem> items(Long templateId) {
        return itemMapper.selectByTemplateId(templateId);
    }

    @Override
    @Transactional
    public Long add(WrTemplate template, List<WrTemplateItem> items) {
        template.setStatus(0);
        templateMapper.insert(template);
        if (items != null) {
            for (WrTemplateItem item : items) {
                item.setTemplateId(template.getId());
                itemMapper.insert(item);
            }
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
    public void replaceItems(Long templateId, List<WrTemplateItem> items) {
        itemMapper.deleteByTemplateId(templateId);
        if (items == null) {
            return;
        }
        for (WrTemplateItem item : items) {
            item.setId(null);
            item.setTemplateId(templateId);
            item.setDelFlag(0);
            itemMapper.insert(item);
        }
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
