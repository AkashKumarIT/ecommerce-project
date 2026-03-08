package com.ecom.cartservice.exception;

import com.ecom.cartservice.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BaseException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("correlationId={} path={} businessError code={} message={}", correlationId, request.getRequestURI(), ex.getErrorCode(), ex.getMessage());

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .error(ErrorDetail.builder().code(ex.getErrorCode()).message(ex.getMessage()).build())
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("correlationId={} path={} missingHeader={}", correlationId, request.getRequestURI(), ex.getHeaderName());

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .error(ErrorDetail.builder().code("MISSING_HEADER").message(ex.getMessage()).build())
                        .timestamp(Instant.now())
                        .correlationId(correlationId)
                        .build()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("correlationId={} path={} unexpectedError", correlationId, request.getRequestURI(), ex);

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .error(ErrorDetail.builder().code("INTERNAL_ERROR").message("Something went wrong").build())
                .timestamp(Instant.now())
                .correlationId(correlationId)
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
