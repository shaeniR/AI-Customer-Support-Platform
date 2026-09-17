package com.lankamart.support.auth;

import com.lankamart.support.user.UserResponse;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response bodies for the auth endpoints. */
public final class AuthDtos {

	private AuthDtos() {
	}

	public record RegisterRequest(
			@NotBlank @Size(max = 100) String name,
			@NotBlank @Email @Size(max = 255) String email,
			// BCrypt only uses the first 72 bytes of a password
			@NotBlank @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password) {
	}

	public record LoginRequest(
			@NotBlank @Email String email,
			@NotBlank String password) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record AuthResponse(
			String tokenType,
			String accessToken,
			long expiresInSeconds,
			String refreshToken,
			UserResponse user) {
	}

}
