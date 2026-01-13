package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model;

import com.example.txprocessor.domain.model.TransactionType;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("transaction_read_view")
@Getter
public class TransactionReadEntity {
    @Id
    private Long id;
    private BigDecimal amount;
    private TransactionType type;
    private Long parentId;
}
