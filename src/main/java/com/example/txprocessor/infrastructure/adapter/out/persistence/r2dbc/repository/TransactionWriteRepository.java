package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository;

import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionWriteEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Repository
public interface TransactionWriteRepository extends ReactiveCrudRepository<TransactionWriteEntity, Long> {
    @Query("""
        INSERT INTO transactions (id, amount, type, parent_id)
        VALUES (:id, :amount, :type, :parentId)
        ON CONFLICT (id)
        DO UPDATE SET
            amount = EXCLUDED.amount,
            type = EXCLUDED.type,
            parent_id = EXCLUDED.parent_id
        """)
    Mono<Void> upsert(
            @Param("id") Long id,
            @Param("amount") BigDecimal amount,
            @Param("type") TransactionType type,
            @Param("parentId") Long parentId
    );
}