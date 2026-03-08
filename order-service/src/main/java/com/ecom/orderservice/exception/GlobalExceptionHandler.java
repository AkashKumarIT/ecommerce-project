package com.ecom.orderservice.exception;

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

        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(Instant.now(), status.value(), ex.getErrorCode(), ex.getMessage(), request.getRequestURI(), traceId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.warn("traceId={} path={} validationFailed={}", traceId, request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED", "Invalid request payload", request.getRequestURI(), traceId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        log.error("traceId={} path={} unexpectedError", traceId, request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR", "Unexpected internal error", request.getRequestURI(), traceId));
    }
}
