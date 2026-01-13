package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs.it;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionReadEntity;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository.TransactionReadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

import java.util.List;

@SpringBootTest
@Import(CustomTestcontainersConfiguration.class)
class TransactionReadRepositoryIT {
    @Autowired
    TransactionReadRepository repository;

    @Autowired
    DatabaseClient databaseClient;

    @BeforeEach
    void setupData() {
        databaseClient.sql("""
        DELETE FROM transactions;

        INSERT INTO transactions (id, amount, type, parent_id) VALUES
        (10, 1000, 'CAR', NULL),
        (11, 500,  'CAR', 10),
        (12, 200,  'CAR', 11);
    """)
                .then()
                .block();
    }

    @Test
    void shouldStreamSubgraphFromRoot() {
        StepVerifier.create(repository.streamSubgraph(10L))
                .expectNextMatches(tx -> tx.getId().equals(10L))
                .expectNextMatches(tx -> tx.getParentId().equals(10L))
                .expectNextMatches(tx -> tx.getParentId().equals(11L))
                .verifyComplete();
    }

    @Test
    void shouldLoadAncestors() {
        StepVerifier.create(repository.findWithParents(12L).collectList())
                .assertNext(list -> {
                    // ids: 12 -> 11 -> 10
                    var ids = list.stream().map(TransactionReadEntity::getId).toList();
                    assert ids.containsAll(List.of(10L, 11L, 12L));
                })
                .verifyComplete();
    }
}