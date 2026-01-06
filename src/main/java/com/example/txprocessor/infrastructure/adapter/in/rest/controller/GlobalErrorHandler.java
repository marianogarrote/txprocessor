package com.example.txprocessor.infrastructure.adapter.in.rest.controller;

import com.example.txprocessor.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestControllerAdvice
public class GlobalErrorHandler extends ResponseEntityExceptionHandler {
    private static final String DEFAULT_ERROR_CODE =  "unknown";
    private static final String DOMAIN_MSG_FORMAT = "[%s] Data error : %s";
    @ExceptionHandler(value = DomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<ApiError>> handleCustomException(DomainException ex, ServerWebExchange exchange) {
        String bodyOfResponse = String.format(DOMAIN_MSG_FORMAT, ex.getType(), ex.getMessage());
        return Mono.just(
                new ResponseEntity<>(
                        new ApiError(
                                "DATA: " +  (ex.getType() != null? ex.getType().name() : DEFAULT_ERROR_CODE),
                                bodyOfResponse,
                                ex.getRootTxId(),
                                ex.getParentTxId()
                        ),
                        BAD_REQUEST
                )
        );
    }

    @ExceptionHandler( {TimeoutException.class, IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseEntity<ApiError>> handleInternalServiceError(Exception ex, ServerWebExchange exchange) {
        String bodyOfResponse = "Internal server error : " + ex.getMessage();
        return Mono.just(
                new ResponseEntity<>(new ApiError("COMMUNICATION", bodyOfResponse), INTERNAL_SERVER_ERROR)
        );
    }

    @ExceptionHandler({Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<ResponseEntity<ApiError>> handleGenericError(Exception ex, ServerWebExchange exchange) {
        String bodyOfResponse = "Internal server error : " + ex.getMessage();
        return Mono.just(
                new ResponseEntity<>(new ApiError("COMMUNICATION", bodyOfResponse), INTERNAL_SERVER_ERROR)
        );
    }

    public record ApiError (String code, String message, Long rootTxId, Long parentTxId) {
        public ApiError(String code, String message) {
            this(code, message, null, null);
        }
    }
}