package com.example.txprocessor.infrastructure.adapter.in.rest.controller.documentation;
import com.example.txprocessor.domain.model.TransactionType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Tag(name = "Transactions", description = "CQRS Command & Query API for transaction processing")
@RequestMapping("/api/v1/transactions")
public interface TransactionApi {

    @Operation(
            summary = "Create or update a transaction (CQRS Command)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Transaction processed successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProcessTransactionOutput.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Domain validation error"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @PutMapping(
            value = "/{transaction_id}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    Mono<ProcessTransactionOutput> processTransaction(
            @Parameter(description = "Transaction identifier", example = "12", required = true)
            @PathVariable("transaction_id")
            Long transactionId,
            @RequestBody(
                    required = true,
                    content = @Content(schema = @Schema(implementation = ProcessTransactionInput.class))
            )
            @org.springframework.web.bind.annotation.RequestBody
            ProcessTransactionInput input
    );

    @Operation(summary = "Search transactions by type (CQRS Query)")
    @GetMapping("/types/{type}")
    Flux<Long> searchByType(
            @Parameter(description = "Transaction type", example = "cars")
            @PathVariable("type") TransactionType type
    );

    @Operation(summary = "Calculate aggregated transaction sum")
    @GetMapping("/sum/{transaction_id}")
    Mono<SumOutput> calculateSum(
            @Parameter(description = "Root transaction identifier", example = "10")
            @PathVariable("transaction_id") Long transactionId
    );

    /***
     * DTOs
     */
    record ProcessTransactionInput(
            @Schema(description = "Transaction amount", example = "10000")
            BigDecimal amount,

            @Schema(description = "Transaction type", example = "shopping")
            TransactionType type,

            @Schema(description = "Optional parent transaction ID", nullable = true)
            @com.fasterxml.jackson.annotation.JsonProperty("parent_id")
            Long parentId
    ) {}

    record ProcessTransactionOutput(
            @Schema(description = "Operation status", example = "ok")
            String status
    ) {
        public static ProcessTransactionOutput ok() {
            return new ProcessTransactionOutput("ok");
        }
    }

    record SumOutput(
            @Schema(description = "Aggregated sum", example = "1500.50")
            BigDecimal sum
    ) {}
}