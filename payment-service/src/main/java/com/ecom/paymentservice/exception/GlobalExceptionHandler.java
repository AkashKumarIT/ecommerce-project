package com.ecom.paymentservice.exception;

import com.ecom.paymentservice.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiErrorResponse> handleBaseException(BaseException ex, HttpServletRequest request) {
        HttpStatus status = "RESOURCE_NOT_FOUND".equals(ex.getErrorCode())
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;

        String traceId = UUID.randomUUID().toString();
        log.warn("traceId={} path={} code={} message={}", traceId, request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(status).body(buildError(status, ex.getErrorCode(), ex.getMessage(), request, traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("traceId={} path={} validationError={}", traceId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(buildError(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Invalid request payload", request, traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("traceId={} path={} Unexpected error", traceId, request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected internal error", request, traceId));
    }

    private ApiErrorResponse buildError(HttpStatus status, String code, String message, HttpServletRequest request, String traceId) {
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                code,
                message,
                request.getRequestURI(),
                traceId
        );
    }
}
