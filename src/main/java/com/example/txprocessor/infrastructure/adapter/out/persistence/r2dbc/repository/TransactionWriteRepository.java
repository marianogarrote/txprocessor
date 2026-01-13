package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository;

import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionWriteEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionWriteRepository extends ReactiveCrudRepository<TransactionWriteEntity, Long> {
}