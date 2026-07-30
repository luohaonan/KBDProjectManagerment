package com.kbd.pms.web;

import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.service.DocumentService;
import com.kbd.pms.service.FileStorageService;
import com.kbd.pms.service.SecurityHelper;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@SuppressWarnings("null")
public class DocumentController {

  private final DocumentService documentService;
  private final FileStorageService fileStorageService;
  private final SecurityHelper securityHelper;

  public DocumentController(
      DocumentService documentService,
      FileStorageService fileStorageService,
      SecurityHelper securityHelper) {
    this.documentService = documentService;
    this.fileStorageService = fileStorageService;
    this.securityHelper = securityHelper;
  }

  @PostMapping("/upload")
  @PreAuthorize("hasRole('ROLE_PM') or hasRole('ROLE_DEPT_HEAD')")
  public Result<DocumentEntity> uploadDocument(
      @RequestParam("file") MultipartFile file,
      @RequestParam("projectId") Long projectId,
      @RequestParam("projectCode") String projectCode,
      @RequestParam("milestonePhase") Enums.MilestoneStage milestonePhase,
      @RequestParam("fileType") String fileType,
      @RequestParam("uploaderId") Long uploaderId) throws IOException {
    DocumentEntity doc = documentService.uploadDocument(file, projectId, projectCode, milestonePhase, fileType, uploaderId);
    return Result.ok(doc);
  }

  @GetMapping("/project/{projectId}/phase/{phase}")
  public Result<List<DocumentEntity>> getDocumentsByProjectAndPhase(
      @PathVariable Long projectId,
      @PathVariable Enums.MilestoneStage phase) {
    List<DocumentEntity> docs = documentService.getDocumentsByProjectAndPhase(projectId, phase);
    return Result.ok(docs);
  }

  @PostMapping("/{id}/review")
  @PreAuthorize("hasRole('ROLE_COMPLIANCE')")
  public Result<Void> reviewDocument(
      @PathVariable Long id,
      @RequestParam Enums.ComplianceStatus status,
      @RequestParam Long reviewerId) {
    documentService.reviewDocument(id, status, reviewerId);
    return Result.ok(null);
  }

  @GetMapping("/pending-reviews")
  @PreAuthorize("hasRole('ROLE_COMPLIANCE')")
  public Result<List<DocumentEntity>> getPendingReviews() {
    List<DocumentEntity> docs = documentService.getPendingReviews();
    return Result.ok(docs);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) throws IOException {
    DocumentEntity doc = documentService.getDocumentById(id);
    documentService.assertCanView(doc);

    Resource resource = fileStorageService.loadFileAsResource(doc.getStoragePath());
    MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(doc.getFileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }

  @GetMapping("/{id}/preview")
  public ResponseEntity<Resource> previewDocument(@PathVariable Long id) throws IOException {
    DocumentEntity doc = documentService.getDocumentById(id);
    documentService.assertCanView(doc);

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
    } else {
      mediaType = MediaType.APPLICATION_OCTET_STREAM;
    }
    return ResponseEntity.ok()
        .contentType(mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(doc.getFileName(), StandardCharsets.UTF_8).build().toString())
        .body(resource);
  }
}
