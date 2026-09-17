package com.lankamart.support.auth;

import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lankamart.support.auth.AuthDtos.AuthResponse;
import com.lankamart.support.auth.AuthDtos.LoginRequest;
import com.lankamart.support.auth.AuthDtos.RegisterRequest;
import com.lankamart.support.common.ApiException;
import com.lankamart.support.user.Role;
import com.lankamart.support.user.User;
import com.lankamart.support.user.UserRepository;
import com.lankamart.support.user.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final String INVALID_CREDENTIALS = "Invalid email or password";

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final TokenGenerator tokenGenerator;
	private final LoginAttemptService loginAttemptService;
	private final AuthProperties properties;

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalize(request.email());
		if (userRepository.existsByEmail(email)) {
			throw ApiException.conflict("An account with this email already exists");
		}
		User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.CUSTOMER);
		return UserResponse.from(userRepository.save(user));
	}

	/** Not @Transactional as a whole: a failed attempt must still be counted in Redis. */
	public AuthResponse login(LoginRequest request) {
		String email = normalize(request.email());
		loginAttemptService.checkNotBlocked(email);

		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			loginAttemptService.loginFailed(email);
			throw ApiException.unauthorized(INVALID_CREDENTIALS);
		}
		if (!user.isEnabled()) {
			throw ApiException.unauthorized("This account is disabled");
		}

		loginAttemptService.loginSucceeded(email);
		return issueTokens(user);
	}

	/** Swaps a valid refresh token for a new pair. The old refresh token cannot be used again. */
	@Transactional
	public AuthResponse refresh(String refreshToken) {
		RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenGenerator.hash(refreshToken))
				.filter(RefreshToken::isUsable)
				.orElseThrow(() -> ApiException.unauthorized("Refresh token is invalid or expired"));
		User user = stored.getUser();
		if (!user.isEnabled()) {
			throw ApiException.unauthorized("This account is disabled");
		}
		stored.setRevoked(true);
		return issueTokens(user);
	}

	@Transactional
	public void logout(String refreshToken) {
		refreshTokenRepository.findByTokenHash(tokenGenerator.hash(refreshToken))
				.ifPresent(token -> token.setRevoked(true));
	}

	private AuthResponse issueTokens(User user) {
		String refreshToken = tokenGenerator.newRefreshToken();
		refreshTokenRepository.save(new RefreshToken(user, tokenGenerator.hash(refreshToken),
				Instant.now().plus(properties.refreshTokenTtl())));
		return new AuthResponse("Bearer", jwtService.createAccessToken(user),
				properties.accessTokenTtl().toSeconds(), refreshToken, UserResponse.from(user));
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
