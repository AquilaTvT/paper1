package com.mmvs.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTaskRequest(
        @NotBlank(message = "videoId 不能为空") String videoId,
        String queryText,
        String instruction
) {

    public String effectiveQueryText() {
        if (queryText != null && !queryText.isBlank()) {
            return queryText.trim();
        }
        if (instruction != null && !instruction.isBlank()) {
            return instruction.trim();
        }
        return "请总结视频中的关键事件。";
    }
}
