package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionEntityMapper;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionReadEntity;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository.TransactionReadRepository;
import com.example.txprocessor.infrastructure.adapter.out.persistence.repository.TransactionReader;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@AllArgsConstructor
public class R2dbcTransactionReadAdapter implements TransactionReader  {
    private final TransactionEntityMapper mapper;
    private final TransactionReadRepository repository;

    @Override
    public Mono<Transaction> findById(Long id) {
        return repository.findWithParents(id)
                .collectList()
                .flatMap(entities -> rebuildGraph(entities).next());
    }

    @Override
    public Flux<Long> findIdsByType(TransactionType type) {
        return repository.findIdsByType(type.name());
    }

    @Override
    public Flux<Transaction> streamSubgraph(Long rootTxId) {
        return repository.streamSubgraph(rootTxId)
                .collectList()
                .flatMapMany(this::rebuildGraph);
    }

    private Flux<Transaction> rebuildGraph(List<TransactionReadEntity> entities) {

        Map<Long, TransactionReadEntity> byId =
                entities.stream()
                        .collect(Collectors.toMap(
                                TransactionReadEntity::getId,
                                Function.identity()
                        ));

        Map<Long, Transaction> cache = new HashMap<>();

        return Flux.fromIterable(entities)
                .map(e -> toDomainRecursive(e, byId, cache));
    }

    private Transaction toDomainRecursive(
            TransactionReadEntity entity,
            Map<Long, TransactionReadEntity> byId,
            Map<Long, Transaction> cache
    ) {
        if (cache.containsKey(entity.getId())) {
            return cache.get(entity.getId());
        }

        Transaction parent = null;
        if (entity.getParentId() != null) {
            parent = toDomainRecursive(
                    byId.get(entity.getParentId()),
                    byId,
                    cache
            );
        }

        Transaction tx = mapper.toDomain(entity, parent);
        cache.put(tx.id(), tx);
        return tx;
    }
}

