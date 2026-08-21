package com.kbd.pms.web;

import com.kbd.pms.dto.DeliverableUploadRequest;
import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.service.FileStorageService;
import com.kbd.pms.service.MilestoneDeliverableService;
import com.kbd.pms.service.SecurityHelper;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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
  private final SecurityHelper securityHelper;

  public MilestoneDeliverableController(
      MilestoneDeliverableService deliverableService,
      FileStorageService fileStorageService,
      SecurityHelper securityHelper) {
    this.deliverableService = deliverableService;
    this.fileStorageService = fileStorageService;
    this.securityHelper = securityHelper;
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

  /** 项目详情交付物管理：跨全部 G0-G9 阶段返回当前用户可见的文件。 */
  @GetMapping("/project/{projectId}")
  public Result<List<DocumentEntity>> getProjectDeliverables(@PathVariable Long projectId) {
    return Result.ok(deliverableService.getVisibleDocuments(projectId));
  }

  /** 项目详情交付物管理：跨 G0-G9 返回固定槽位分组，包含空槽位占位。 */
  @GetMapping("/project/{projectId}/slots")
  public Result<List<MilestoneDeliverableService.MilestoneDeliverableGroupVO>> getProjectDeliverableSlots(
      @PathVariable Long projectId) {
    return Result.ok(deliverableService.getProjectDeliverableSlotGroups(projectId));
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

  @PostMapping("/import")
  public Result<DocumentEntity> importDeliverable(
      @RequestParam("file") MultipartFile file,
      @RequestParam("projectId") Long projectId,
      @RequestParam("milestoneCode") String milestoneCode,
      @RequestParam("slotCode") String slotCode,
      @RequestParam(value = "fileType", required = false) String fileType) throws IOException {
    return Result.ok(deliverableService.importDeliverable(file, projectId, milestoneCode, slotCode, fileType));
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
    deliverableService.assertCanView(doc);

    Resource resource = fileStorageService.loadFileAsResource(doc.getStoragePath());
    MediaType mediaType = mediaTypeFor(doc.getFileName());
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(doc.getFileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }

  /**
   * 预览交付物文件
   */
  @GetMapping("/{documentId}/preview")
  public ResponseEntity<Resource> previewDeliverable(@PathVariable Long documentId) throws IOException {
    DocumentEntity doc = deliverableService.getDocumentById(documentId);
    deliverableService.assertCanView(doc);

    Resource resource = fileStorageService.loadFileAsResource(doc.getStoragePath());
    String fileName = doc.getFileName().toLowerCase();
    MediaType mediaType;
    if (fileName.endsWith(".pdf")) {
      mediaType = MediaType.APPLICATION_PDF;
    } else if (fileName.endsWith(".png")) {
      mediaType = MediaType.IMAGE_PNG;
    } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
      mediaType = MediaType.IMAGE_JPEG;
    } else if (fileName.endsWith(".gif")) {
      mediaType = MediaType.IMAGE_GIF;
    } else if (fileName.endsWith(".txt") || fileName.endsWith(".csv") || fileName.endsWith(".log")) {
      mediaType = MediaType.TEXT_PLAIN;
    } else if (fileName.endsWith(".docx")) {
      mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    } else if (fileName.endsWith(".xlsx")) {
      mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    } else if (fileName.endsWith(".xls")) {
      mediaType = MediaType.parseMediaType("application/vnd.ms-excel");
    } else {
      throw new com.kbd.pms.exception.ApiException(400, "该文件类型不支持在线预览");
    }

    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(doc.getFileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }

  private MediaType mediaTypeFor(String fileName) {
    String lowerName = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
    if (lowerName.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
    if (lowerName.endsWith(".png")) return MediaType.IMAGE_PNG;
    if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
    if (lowerName.endsWith(".gif")) return MediaType.IMAGE_GIF;
    if (lowerName.endsWith(".txt") || lowerName.endsWith(".csv") || lowerName.endsWith(".log")) {
      return MediaType.TEXT_PLAIN;
    }
    if (lowerName.endsWith(".docx")) {
      return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }
    if (lowerName.endsWith(".xlsx")) {
      return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
    if (lowerName.endsWith(".xls")) return MediaType.parseMediaType("application/vnd.ms-excel");
    return MediaType.APPLICATION_OCTET_STREAM;
  }
}