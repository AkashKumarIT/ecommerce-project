package com.ecom.cartservice.exception;

import ch.qos.logback.core.net.SocketConnector;
import com.ecom.cartservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.requests.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<?>> handleBusinessException(BaseException ex) {

        log.warn("Business exception: {}", ex.getMessage());

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(ex.getErrorCode())
                        .message(ex.getMessage())
                        .build())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingHeader(
            MissingRequestHeaderException ex) {

        return ResponseEntity.badRequest().body(
                ApiResponse.builder()
                        .success(false)
                        .error(ErrorDetail.builder()
                                .code("MISSING_HEADER")
                                .message(ex.getMessage())
                                .build()
                        )
                        .build()
        );
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {

        log.error("Unexpected error occurred", ex);

        ApiResponse<?> response = ApiResponse.builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code("INTERNAL_ERROR")
                        .message("Something went wrong")
                        .build())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
