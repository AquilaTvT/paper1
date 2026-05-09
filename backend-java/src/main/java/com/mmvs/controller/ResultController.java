package com.mmvs.controller;

import com.mmvs.dto.ApiResponse;
import com.mmvs.dto.ResultResponse;
import com.mmvs.service.ResultService;
import com.mmvs.util.IdGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/results")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping("/{taskId}")
    public ApiResponse<ResultResponse> getResult(@PathVariable String taskId) {
        return ApiResponse.ok(ResultResponse.from(resultService.getResult(taskId)), IdGenerator.requestId());
    }
}
