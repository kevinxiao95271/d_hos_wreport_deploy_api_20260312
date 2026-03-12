package com.kxhospital.wreport.controller;

import com.kxhospital.wreport.common.R;
import com.kxhospital.wreport.pojo.response.AttachmentVO;
import com.kxhospital.wreport.service.WrAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "附件管理")
@RestController
@RequestMapping("/wr/attachment")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class WrAttachmentController {

    private final WrAttachmentService attachmentService;

    @Operation(summary = "上传佐证附件")
    @PostMapping("/upload")
    public R<AttachmentVO> upload(@RequestParam Long recordId,
                                  @RequestParam(required = false) Long itemId,
                                  @RequestParam("file") MultipartFile file) {
        return R.ok(attachmentService.upload(recordId, itemId, file));
    }

    @Operation(summary = "删除附件")
    @PostMapping("/delete/{id}")
    public R<Void> delete(@PathVariable Long id) {
        attachmentService.delete(id);
        return R.ok();
    }

    @Operation(summary = "查询记录的附件列表")
    @GetMapping("/list/{recordId}")
    public R<List<AttachmentVO>> list(@PathVariable Long recordId) {
        return R.ok(attachmentService.listByRecord(recordId));
    }
}
