package com.lankamart.support.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Values from the "app.auth" section of application.yml. */
@ConfigurationProperties(prefix = "app.auth")
public record AuthProperties(
		String jwtSecret,
		Duration accessTokenTtl,
		Duration refreshTokenTtl,
		int maxFailedLogins,
		Duration loginLockDuration) {
}
