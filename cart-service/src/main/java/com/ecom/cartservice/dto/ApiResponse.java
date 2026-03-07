package com.ecom.cartservice.dto;

import com.ecom.cartservice.exception.ErrorDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T>{
    private boolean success;
    private T data;
    private ErrorDetail error;
    private Instant timestamp;
    private String correlationId;
}
