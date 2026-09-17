package com.lankamart.support;

import org.springframework.boot.SpringApplication;

public class TestSupportApplication {

	public static void main(String[] args) {
		SpringApplication.from(SupportApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
