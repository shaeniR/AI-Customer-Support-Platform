package com.lankamart.support.common;

import org.springframework.http.HttpStatus;

import lombok.Getter;

/**
 * Business error with an HTTP status. Turned into the standard JSON error by {@link GlobalExceptionHandler}.
 */
@Getter
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final Long retryAfterSeconds;

	public ApiException(HttpStatus status, String message) {
		this(status, message, null);
	}

	public ApiException(HttpStatus status, String message, Long retryAfterSeconds) {
		super(message);
		this.status = status;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public static ApiException notFound(String message) {
		return new ApiException(HttpStatus.NOT_FOUND, message);
	}

	public static ApiException conflict(String message) {
		return new ApiException(HttpStatus.CONFLICT, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(HttpStatus.UNAUTHORIZED, message);
	}

	public static ApiException tooManyRequests(String message, long retryAfterSeconds) {
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, retryAfterSeconds);
	}

}
