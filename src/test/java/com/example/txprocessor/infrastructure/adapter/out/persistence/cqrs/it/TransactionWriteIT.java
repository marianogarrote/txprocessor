package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs.it;

import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs.it.support.TestcontainersConfig;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository.TransactionWriteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

@SpringBootTest
@Import(TestcontainersConfig.class)
class TransactionWriteIT {

    @Autowired
    TransactionWriteRepository repository;

    @Test
    void upsert_shouldInsertOrUpdate() {
        StepVerifier.create(
                repository.upsert(1L, BigDecimal.TWO, TransactionType.cars, null)
        ).verifyComplete();
    }
}