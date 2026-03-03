package com.mindrevol.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.mindrevol.backend.config.TestRedisConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
class MindrevolBackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
