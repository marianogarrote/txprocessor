package com.example.txprocessor.application.port.in;

import com.example.txprocessor.domain.model.TransactionType;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

public interface ProcessTransactionUseCase {
    Mono<Void> invoke(Long id, BigDecimal amount, TransactionType type, Long parentId);
}