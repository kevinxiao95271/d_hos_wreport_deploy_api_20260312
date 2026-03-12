package com.kxhospital.wreport.service;

import com.kxhospital.wreport.entity.WrAttachment;
import com.kxhospital.wreport.pojo.response.AttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface WrAttachmentService {
    AttachmentVO upload(Long recordId, Long itemId, MultipartFile file);
    void delete(Long attachmentId);
    List<AttachmentVO> listByRecord(Long recordId);
}
