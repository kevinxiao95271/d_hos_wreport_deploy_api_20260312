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

import javax.servlet.http.HttpServletResponse;
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

    @Operation(summary = "打包下载记录所有附件（ZIP）",
               description = "将指定填报记录的全部附件打包为 ZIP 流式返回。\n" +
                             "ZIP 文件名格式：{任务名}_{机构名}_{yyyyMMdd}.zip\n" +
                             "ZIP 内按 headerPath 层级分目录存放，同目录同名文件自动追加序号。\n" +
                             "管理员及该机构用户均可调用；响应为二进制流，Content-Disposition 携带文件名。")
    @GetMapping("/download/zip/{recordId}")
    public void downloadZip(@PathVariable Long recordId, HttpServletResponse response) {
        attachmentService.downloadZip(recordId, response);
    }
}
