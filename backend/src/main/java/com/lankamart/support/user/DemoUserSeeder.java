package com.lankamart.support.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Creates one demo account per role on startup, if it does not exist yet. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed-users", havingValue = "true")
public class DemoUserSeeder implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${app.demo.password}")
	private String demoPassword;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seed("Demo Customer", "customer@lankamart.lk", Role.CUSTOMER);
		seed("Demo Agent", "agent@lankamart.lk", Role.AGENT);
		seed("Demo Admin", "admin@lankamart.lk", Role.ADMIN);
	}

	private void seed(String name, String email, Role role) {
		if (userRepository.existsByEmail(email)) {
			return;
		}
		userRepository.save(new User(name, email, passwordEncoder.encode(demoPassword), role));
		log.info("Created demo {} account: {}", role, email);
	}

}
