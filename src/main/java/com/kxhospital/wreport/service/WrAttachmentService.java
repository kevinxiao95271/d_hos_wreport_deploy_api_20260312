package com.kxhospital.wreport.service;

import com.kxhospital.wreport.pojo.response.AttachmentVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

public interface WrAttachmentService {
    AttachmentVO upload(Long recordId, Long itemId, MultipartFile file);
    void delete(Long attachmentId);
    List<AttachmentVO> listByRecord(Long recordId);

    /**
     * 将指定填报记录的所有附件打包为 ZIP 并写入 HTTP 响应流。
     * ZIP 文件名格式：{任务名}_{机构名}_{yyyyMMdd}.zip
     * ZIP 内部按 headerPath 分目录存放。
     */
    void downloadZip(Long recordId, HttpServletResponse response);
}
