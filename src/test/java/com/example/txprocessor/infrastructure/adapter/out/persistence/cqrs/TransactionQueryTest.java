package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.infrastructure.adapter.out.persistence.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransactionQueryTest {
    private static final Transaction TX = TransactionFactory.sample();

    @Mock
    private TransactionRepository repository;

    private TransactionQuery query;

    @BeforeEach
    void setup() {
        query = new TransactionQuery(repository);
    }

    @Test
    void findById_delegates_to_repository() {

        Mockito.when(repository.findById(1L)).thenReturn(Mono.just(TX));

        StepVerifier.create(query.findById(1L))
                .expectNext(TX)
                .verifyComplete();
    }

    @Test
    void findIdsByType_delegates_to_repository() {
        Mockito.when(repository.findIdsByType(TransactionType.cars)).thenReturn(Flux.just(1L, 2L));

        StepVerifier.create(query.findIdsByType(TransactionType.cars))
                .expectNext(1L, 2L)
                .verifyComplete();
    }

    @Test
    void streamSubgraph_delegates_to_repository() {
        Mockito.when(repository.streamSubgraph(1L)).thenReturn(Flux.just(TX));

        StepVerifier.create(query.streamSubgraph(1L))
                .expectNext(TX)
                .verifyComplete();
    }
}