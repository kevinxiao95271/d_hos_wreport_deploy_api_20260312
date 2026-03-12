package com.kxhospital.wreport.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kxhospital.wreport.entity.WrTemplate;
import com.kxhospital.wreport.entity.WrTemplateItem;
import com.kxhospital.wreport.entity.WrTemplateRow;
import com.kxhospital.wreport.pojo.response.TemplateDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WrTemplateService {
    IPage<WrTemplate> page(Page<WrTemplate> page, String templateName, Integer status);
    WrTemplate detail(Long id);
    TemplateDetailVO detailFull(Long id);
    List<WrTemplateItem> items(Long templateId);
    Long add(WrTemplate template, List<WrTemplateItem> items);
    void update(WrTemplate template);
    void updateStatus(Long id, Integer status);
    void delete(Long id);
    String uploadFormatTemplate(Long itemId, MultipartFile file);
    void deleteFormatTemplate(Long itemId);
    void replaceItems(Long templateId, List<WrTemplateItem> items);
    List<WrTemplate> listActive();
    List<WrTemplateRow> listRows(Long templateId);
    void replaceRows(Long templateId, List<WrTemplateRow> rows);
}
