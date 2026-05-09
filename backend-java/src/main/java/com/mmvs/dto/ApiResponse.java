package com.mmvs.dto;

public record ApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        String requestId
) {

    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>(true, data, "ok", null, requestId);
    }

    public static <T> ApiResponse<T> fail(String message, String errorCode, String requestId) {
        return new ApiResponse<>(false, null, message, errorCode, requestId);
    }
}
