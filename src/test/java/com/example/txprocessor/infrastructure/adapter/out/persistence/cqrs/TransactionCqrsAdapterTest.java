package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
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
class TransactionCqrsAdapterTest {
    private static final Transaction TX = TransactionFactory.sample();

    @Mock
    private TransactionCommand command;

    @Mock
    private TransactionQuery query;

    private TransactionCqrsAdapter adapter = new TransactionCqrsAdapter(command, query);

    @BeforeEach
    void setUp() {
        adapter = new TransactionCqrsAdapter(command, query);
    }

    @Test
    void findById_delegates_to_query() {
        Mockito.when(query.findById(1L)).thenReturn(Mono.just(TX));

        StepVerifier.create(adapter.findById(1L))
                .expectNext(TX)
                .verifyComplete();
    }

    @Test
    void findIdsByType_delegates_to_query() {
        Mockito.when(query.findIdsByType(TransactionType.cars)).thenReturn(Flux.just(1L));

        StepVerifier.create(adapter.findIdsByType(TransactionType.cars))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void streamSubgraph_delegates_to_query() {
        Mockito.when(query.streamSubgraph(1L)).thenReturn(Flux.just(TX));

        StepVerifier.create(adapter.streamSubgraph(1L))
                .expectNext(TX)
                .verifyComplete();
    }

    @Test
    void save_delegates_to_command() {
        Mockito.when(command.save(TX)).thenReturn(Mono.just(TX));

        StepVerifier.create(adapter.save(TX))
                .expectNext(TX)
                .verifyComplete();
    }
}