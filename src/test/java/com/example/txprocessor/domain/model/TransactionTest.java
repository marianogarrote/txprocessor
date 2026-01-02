package com.example.txprocessor.domain.model;

import com.example.txprocessor.domain.exception.CircularTransactionCreationException;
import com.example.txprocessor.domain.exception.DomainErrorType;
import com.example.txprocessor.domain.exception.TransactionCreationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TransactionTest {

    @Test
    @DisplayName("Transaction can be created without parent")
    public void createTransactionWithoutParent() {
        Transaction tx = new Transaction(10L, BigDecimal.TEN, TransactionType.cards, null);
        assertThat(tx.parentId()).isNull();
    }

    @Test
    @DisplayName("Transaction created with valid parent")
    public void createTransactionWithValidParent() {
        Long parentId = 11L;
        Transaction tx = new Transaction(10L, BigDecimal.TEN, TransactionType.cards, parentId);
        assertThat(tx.parentId()).isEqualTo(parentId);
    }

    @Test
    @DisplayName("Transaction created with circular parent reference")
    public void createTransactionWithCircularParent() {
        Long id = 11L;
        assertThatThrownBy(() -> new Transaction(id, BigDecimal.TEN, TransactionType.cards, id))
                .isInstanceOf(CircularTransactionCreationException.class)
                .hasMessage("Transaction cannot be parent of itself [id = 11]")
                .hasFieldOrPropertyWithValue("type", DomainErrorType.circularTransactionCreation);
    }

    @Test
    @DisplayName("Transaction created with null id")
    public void createTransactionWithNullId() {
        assertThatThrownBy(() -> new Transaction(null, BigDecimal.TEN, TransactionType.cards, null))
                .isInstanceOf(TransactionCreationException.class)
                .hasMessage("Transaction creation: Invalid value for field 'id' = 'null'")
                .hasFieldOrPropertyWithValue("type", DomainErrorType.invalidFieldValueDuringTxCreation);
    }

    @Test
    @DisplayName("Transaction created with null transaction type")
    public void createTransactionWithNullTxType() {
        assertThatThrownBy(() -> new Transaction(1L, BigDecimal.TEN, null, null))
                .isInstanceOf(TransactionCreationException.class)
                .hasMessage("Transaction creation: Invalid value for field 'type' = 'null'")
                .hasFieldOrPropertyWithValue("type", DomainErrorType.invalidFieldValueDuringTxCreation);
    }

    @Test
    @DisplayName("Transaction created with null amount")
    public void createTransactionWithNullAmount() {
        assertThatThrownBy(() -> new Transaction(1L, null, TransactionType.cards, null))
                .isInstanceOf(TransactionCreationException.class)
                .hasMessage("Transaction creation: Invalid value for field 'amount' = 'null'")
                .hasFieldOrPropertyWithValue("type", DomainErrorType.invalidFieldValueDuringTxCreation);
    }
}
