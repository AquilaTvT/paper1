package com.mmvs.dto;

import com.mmvs.model.VideoFile;
import java.time.Instant;

public record UploadVideoResponse(
        String videoId,
        String originalFileName,
        String storedPath,
        long fileSize,
        String contentType,
        Instant createdAt
) {

    public static UploadVideoResponse from(VideoFile videoFile) {
        return new UploadVideoResponse(
                videoFile.getVideoId(),
                videoFile.getOriginalFileName(),
                videoFile.getStoredPath(),
                videoFile.getFileSize(),
                videoFile.getContentType(),
                videoFile.getCreatedAt()
        );
    }
}
