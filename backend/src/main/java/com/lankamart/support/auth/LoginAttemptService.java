package com.lankamart.support.auth;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.lankamart.support.common.ApiException;

import lombok.RequiredArgsConstructor;

/**
 * Counts failed logins per email in Redis. After too many failures the email is
 * blocked until the Redis key expires.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

	private static final String KEY_PREFIX = "login:failed:";

	private final StringRedisTemplate redis;
	private final AuthProperties properties;

	public void checkNotBlocked(String email) {
		String key = key(email);
		String value = redis.opsForValue().get(key);
		if (value != null && Integer.parseInt(value) >= properties.maxFailedLogins()) {
			Long secondsLeft = redis.getExpire(key, TimeUnit.SECONDS);
			long retryAfter = (secondsLeft == null || secondsLeft < 0) ? properties.loginLockDuration().toSeconds()
					: secondsLeft;
			throw ApiException.tooManyRequests("Too many failed login attempts. Try again later.", retryAfter);
		}
	}

	public void loginFailed(String email) {
		String key = key(email);
		Long attempts = redis.opsForValue().increment(key);
		// Start the window on the first failure, and restart it when the block begins
		if (attempts != null && (attempts == 1 || attempts >= properties.maxFailedLogins())) {
			redis.expire(key, properties.loginLockDuration());
		}
	}

	public void loginSucceeded(String email) {
		redis.delete(key(email));
	}

	private static String key(String email) {
		return KEY_PREFIX + email;
	}

}
