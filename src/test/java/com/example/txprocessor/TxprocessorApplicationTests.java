package com.example.txprocessor;

import com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs.it.CustomTestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(CustomTestcontainersConfiguration.class)
@SpringBootTest
class TxprocessorApplicationTests {

	@Test
	void contextLoads() {
	}

}
