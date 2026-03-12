package com.kxhospital.wreport.service.impl;

import com.kxhospital.wreport.config.MinioProperties;
import com.kxhospital.wreport.config.MinioService;
import com.kxhospital.wreport.entity.WrAttachment;
import com.kxhospital.wreport.mapper.WrAttachmentMapper;
import com.kxhospital.wreport.pojo.response.AttachmentVO;
import com.kxhospital.wreport.service.WrAttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WrAttachmentServiceImpl implements WrAttachmentService {

    private final WrAttachmentMapper attachmentMapper;
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
        return attachmentMapper.selectByRecordId(recordId).stream().map(a -> {
            AttachmentVO vo = new AttachmentVO();
            BeanUtils.copyProperties(a, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
