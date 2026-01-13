package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs.it.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16")
                .withDatabaseName("txdb")
                .withUsername("txuser")
                .withPassword("txpass");
    }
}