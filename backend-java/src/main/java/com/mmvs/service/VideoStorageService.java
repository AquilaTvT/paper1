package com.mmvs.service;

import com.mmvs.config.StorageProperties;
import com.mmvs.exception.BusinessException;
import com.mmvs.model.VideoFile;
import com.mmvs.util.IdGenerator;
import com.mmvs.util.SafeFileNameGenerator;
import com.mmvs.util.TimeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoStorageService {

    private final StorageProperties storageProperties;
    private final Path uploadRoot;
    private final Map<String, VideoFile> videos = new ConcurrentHashMap<>();

    public VideoStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
        this.uploadRoot = Path.of(storageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    public VideoFile store(MultipartFile file) {
        validateFile(file);

        String videoId = IdGenerator.videoId();
        String originalFileName = file.getOriginalFilename() == null ? "video" : file.getOriginalFilename();
        String extension = SafeFileNameGenerator.extensionOf(originalFileName);
        String relativePath = SafeFileNameGenerator.storedRelativePath(videoId, originalFileName);
        Path targetPath = uploadRoot.resolve(relativePath).normalize();

        if (!targetPath.startsWith(uploadRoot)) {
            throw new BusinessException("VIDEO_INVALID_PATH", "视频保存路径非法", HttpStatus.BAD_REQUEST);
        }

        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException("VIDEO_SAVE_FAILED", "视频文件保存失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        VideoFile videoFile = new VideoFile();
        videoFile.setVideoId(videoId);
        videoFile.setOriginalFileName(originalFileName);
        videoFile.setStoredFileName(targetPath.getFileName().toString());
        videoFile.setStoredPath(targetPath.toString());
        videoFile.setFileSize(file.getSize());
        videoFile.setExtension(extension);
        videoFile.setContentType(file.getContentType());
        videoFile.setCreatedAt(TimeUtils.now());
        videos.put(videoId, videoFile);
        return videoFile;
    }

    public VideoFile getRequiredVideo(String videoId) {
        return findVideo(videoId)
                .orElseThrow(() -> new BusinessException("VIDEO_NOT_FOUND", "视频不存在：" + videoId, HttpStatus.NOT_FOUND));
    }

    public Optional<VideoFile> findVideo(String videoId) {
        return Optional.ofNullable(videos.get(videoId));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("VIDEO_EMPTY", "上传视频文件不能为空", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new BusinessException("VIDEO_TOO_LARGE", "视频文件超过系统允许大小", HttpStatus.PAYLOAD_TOO_LARGE);
        }

        String extension = SafeFileNameGenerator.extensionOf(file.getOriginalFilename());
        boolean allowed = storageProperties.getAllowedExtensions().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(extension));
        if (!allowed) {
            throw new BusinessException("VIDEO_INVALID_FORMAT", "仅支持 mp4、mov、avi、mkv 视频格式", HttpStatus.BAD_REQUEST);
        }
    }
}
