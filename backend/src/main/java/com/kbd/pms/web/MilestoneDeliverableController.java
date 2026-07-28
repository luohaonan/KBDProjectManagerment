package com.kbd.pms.web;

import com.kbd.pms.dto.DeliverableUploadRequest;
import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.service.FileStorageService;
import com.kbd.pms.service.MilestoneDeliverableService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 里程碑交付物控制器
 *
 * <p>为里程碑控制台提供按槽位管理交付物的 API。</p>
 */
@RestController
@RequestMapping("/api/milestone-deliverables")
@SuppressWarnings("null")
public class MilestoneDeliverableController {

  private final MilestoneDeliverableService deliverableService;
  private final FileStorageService fileStorageService;

  public MilestoneDeliverableController(
      MilestoneDeliverableService deliverableService,
      FileStorageService fileStorageService) {
    this.deliverableService = deliverableService;
    this.fileStorageService = fileStorageService;
  }

  /**
   * 获取指定项目指定里程碑阶段的交付物清单（含已上传状态）
   */
  @GetMapping("/project/{projectId}/milestone/{milestoneCode}")
  public Result<List<MilestoneDeliverableService.DeliverableSlotVO>> getDeliverableSlots(
      @PathVariable Long projectId,
      @PathVariable String milestoneCode) {
    List<MilestoneDeliverableService.DeliverableSlotVO> slots =
        deliverableService.getDeliverableSlots(projectId, milestoneCode);
    return Result.ok(slots);
  }

  /**
   * 上传交付物到指定槽位
   */
  @PostMapping("/upload")
  public Result<DocumentEntity> uploadDeliverable(
      @RequestParam("file") MultipartFile file,
      @RequestParam("projectId") Long projectId,
      @RequestParam("milestoneCode") String milestoneCode,
      @RequestParam("slotCode") String slotCode) throws IOException {

    DeliverableUploadRequest request = new DeliverableUploadRequest(
        null, projectId, milestoneCode, slotCode, null, null);

    DocumentEntity doc = deliverableService.uploadDeliverable(file, request);
    return Result.ok(doc);
  }

  /**
   * 删除交付物
   */
  @DeleteMapping("/{documentId}")
  public Result<Void> deleteDeliverable(@PathVariable Long documentId) throws IOException {
    deliverableService.deleteDeliverable(documentId);
    return Result.ok(null);
  }

  /**
   * 下载交付物文件
   */
  @GetMapping("/{documentId}/download")
  public ResponseEntity<Resource> downloadDeliverable(@PathVariable Long documentId) throws IOException {
    DocumentEntity doc = deliverableService.getDocumentById(documentId);

    Resource resource = fileStorageService.loadFileAsResource(doc.getStoragePath());
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + doc.getFileName() + "\"")
        .body(resource);
  }
}