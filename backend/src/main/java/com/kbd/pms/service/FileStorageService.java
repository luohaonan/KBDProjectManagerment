package com.kbd.pms.service;

import com.kbd.pms.entity.DocumentEntity;
import com.kbd.pms.entity.Enums;
import com.kbd.pms.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@SuppressWarnings("null")
public class FileStorageService {

  @Value("${app.file-storage.root:C:/KBD_PMS/uploads/}")
  private String storageRoot;

  public FileStorageService() {
  }

  public String storeFile(MultipartFile file, String projectCode, Enums.MilestoneStage milestonePhase, Long uploaderId) throws IOException {
    // 创建目录结构：项目代号/里程碑阶段/
    Path projectDir = Paths.get(storageRoot, projectCode, milestonePhase.name());
    Files.createDirectories(projectDir);

    // 生成唯一文件名
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null) {
      originalFilename = "unknown";
    }
    String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
    String uniqueFilename = UUID.randomUUID().toString() + extension;
    Path filePath = projectDir.resolve(uniqueFilename);

    // 存储文件
    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

    return filePath.toString();
  }

  public Resource loadFileAsResource(String filePath) throws IOException {
    Path path = Paths.get(filePath);
    java.net.URI uri = path.toUri();
    Resource resource = new UrlResource(uri);
    if (resource.exists() || resource.isReadable()) {
      return resource;
    } else {
      throw new ApiException(404, "文件不存在或无法读取");
    }
  }

  public void deleteFile(String filePath, Long userId) throws IOException {
    Path path = Paths.get(filePath);
    Files.deleteIfExists(path);

  }

}