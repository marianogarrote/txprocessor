package com.example.txprocessor.infrastructure.config;

import com.example.txprocessor.application.port.in.CalculateSumUseCase;
import com.example.txprocessor.application.port.in.ProcessTransactionUseCase;
import com.example.txprocessor.application.port.in.SearchTxIdByTypeUseCase;
import com.example.txprocessor.application.port.out.TransactionPort;
import com.example.txprocessor.application.usecase.CalculateSumUseOperation;
import com.example.txprocessor.application.usecase.ProcessTransactionOperation;
import com.example.txprocessor.application.usecase.SearchTxIdByTypeOperation;
import com.example.txprocessor.domain.service.TransactionService;
import com.example.txprocessor.infrastructure.adapter.out.persistence.memory.InMemoryTransactionPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TxProcessorAppConfig {

    @Bean
    public TransactionPort transactionPort() {
        return new InMemoryTransactionPort();
    }

    @Bean
    public TransactionService transactionService(TransactionPort transactionPort) {
        return new TransactionService(transactionPort);
    }

    @Bean
    public SearchTxIdByTypeUseCase searchTxIdByTypeUseCase(TransactionService service) {
        return new SearchTxIdByTypeOperation(service);
    }

    @Bean
    public ProcessTransactionUseCase processTransactionUseCase(TransactionService service) {
        return new ProcessTransactionOperation(service);
    }

    @Bean
    public CalculateSumUseCase calculateSumUseCase(TransactionService service) {
        return new CalculateSumUseOperation(service);
    }
}
