package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.infrastructure.adapter.out.persistence.repository.TransactionWriter;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransactionCommandTest {

    private static final Transaction TX = TransactionFactory.sample();

    @Mock
    private TransactionWriter repository;

    @InjectMocks
    private TransactionCommand command;

    @Test
    void save_delegates_to_repository() {
        Mockito.when(repository.save(TX)).thenReturn(Mono.just(TX));

        StepVerifier.create(command.save(TX))
                .expectNext(TX)
                .verifyComplete();

        Mockito.verify(repository).save(TX);
    }
}