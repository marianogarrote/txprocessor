package com.example.txprocessor.domain.model;

import com.example.txprocessor.domain.exception.DomainErrorType;
import com.example.txprocessor.domain.exception.TransactionCreationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTest {

    @Test
    @DisplayName("Transaction can be created without parent")
    void createTransactionWithoutParent() {
        Transaction tx = new Transaction(10L, BigDecimal.TEN, TransactionType.cars, null);
        assertThat(tx.parent()).isNull();
    }

    @Test
    @DisplayName("Transaction created with valid parent")
    void createTransactionWithValidParent() {
        Transaction parent = new Transaction(11L, BigDecimal.ONE, TransactionType.cars, null);
        Transaction tx = new Transaction(10L, BigDecimal.TEN, TransactionType.cars, parent);
        assertThat(tx.parent()).isEqualTo(parent);
    }

    @Test
    @DisplayName("Transaction created with null id")
    void createTransactionWithNullId() {
        assertThatThrownBy(() -> new Transaction(null, BigDecimal.TEN, TransactionType.cars, null)
        )
                .isInstanceOf(TransactionCreationException.class)
                .hasFieldOrPropertyWithValue("type", DomainErrorType.invalidFieldValueDuringTxCreation);
    }

    @Test
    @DisplayName("Transaction created with null transaction type")
    void createTransactionWithNullTxType() {
        assertThatThrownBy(() ->new Transaction(1L, BigDecimal.TEN, null, null))
                .isInstanceOf(TransactionCreationException.class)
                .hasFieldOrPropertyWithValue("type",DomainErrorType.invalidFieldValueDuringTxCreation);
    }

    @Test
    @DisplayName("Transaction created with null amount")
    void createTransactionWithNullAmount() {
        assertThatThrownBy(() ->new Transaction(1L, null, TransactionType.cars, null))
                .isInstanceOf(TransactionCreationException.class)
                .hasFieldOrPropertyWithValue("type",DomainErrorType.invalidFieldValueDuringTxCreation);
    }
}