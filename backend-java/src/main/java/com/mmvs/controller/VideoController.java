package com.mmvs.controller;

import com.mmvs.dto.ApiResponse;
import com.mmvs.dto.UploadVideoResponse;
import com.mmvs.model.VideoFile;
import com.mmvs.service.VideoStorageService;
import com.mmvs.util.IdGenerator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoStorageService videoStorageService;

    public VideoController(VideoStorageService videoStorageService) {
        this.videoStorageService = videoStorageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadVideoResponse> upload(@RequestPart("file") MultipartFile file) {
        VideoFile videoFile = videoStorageService.store(file);
        return ApiResponse.ok(UploadVideoResponse.from(videoFile), IdGenerator.requestId());
    }
}
