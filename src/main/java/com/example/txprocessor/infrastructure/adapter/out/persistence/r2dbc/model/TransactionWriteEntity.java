package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model;

import com.example.txprocessor.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("transactions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionWriteEntity {

    @Id
    private Long id;

    @Column("amount")
    private BigDecimal amount;

    @Column("type")
    private TransactionType type;

    @Column("parent_id")
    private Long parentId;
}
