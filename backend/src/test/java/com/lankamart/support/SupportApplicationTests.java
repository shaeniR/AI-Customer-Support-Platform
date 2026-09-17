package com.lankamart.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Starts the full application against real MySQL and Redis containers.
 * Skipped automatically when Docker is not available (it always runs in CI).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SupportApplicationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoadsAndFlywayMigrationsRun() {
		String baseline = jdbcTemplate.queryForObject(
				"SELECT info_value FROM app_info WHERE info_key = 'schema_baseline'", String.class);
		assertThat(baseline).isEqualTo("1");
	}

}
