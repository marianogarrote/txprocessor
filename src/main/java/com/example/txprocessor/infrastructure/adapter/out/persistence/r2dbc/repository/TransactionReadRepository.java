package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository;

import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionReadEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface TransactionReadRepository extends ReactiveCrudRepository<TransactionReadEntity, Long> {

    @Query("SELECT id FROM transaction_read_view WHERE type = :type")
    Flux<Long> findIdsByType(String type);

    @Query("""
        WITH RECURSIVE ancestors AS (
            SELECT * 
            FROM transaction_read_view 
            WHERE id = :id

            UNION ALL

            SELECT p.*
            FROM transaction_read_view p
            JOIN ancestors a ON a.parent_id = p.id
        )
        SELECT * FROM ancestors
    """)
    Flux<TransactionReadEntity> findWithParents(Long id);

    @Query("""
        WITH RECURSIVE subgraph AS (
            SELECT * FROM transaction_read_view WHERE id = :root
            UNION ALL
            SELECT t.* FROM transaction_read_view t
            JOIN subgraph s ON t.parent_id = s.id
        )
        SELECT * FROM subgraph
    """)
    Flux<TransactionReadEntity> streamSubgraph(Long root);
}