package com.example.txprocessor.application.port.in;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;

public interface CalculateSumUseCase {
    Mono<BigDecimal> invoke(Long txId);
}