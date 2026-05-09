package com.mmvs.exception;

import com.mmvs.dto.ApiResponse;
import com.mmvs.util.IdGenerator;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getHttpStatus())
                .body(ApiResponse.fail(exception.getMessage(), exception.getErrorCode(), IdGenerator.requestId()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception exception) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(exception.getMessage(), "VALIDATION_ERROR", IdGenerator.requestId()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail("视频文件超过系统允许大小", "VIDEO_TOO_LARGE", IdGenerator.requestId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("系统内部错误：" + exception.getMessage(), "INTERNAL_ERROR", IdGenerator.requestId()));
    }
}
