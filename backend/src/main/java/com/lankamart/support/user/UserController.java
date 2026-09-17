package com.lankamart.support.user;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lankamart.support.common.ApiException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

	private final UserRepository userRepository;

	/** Any logged-in user. */
	@GetMapping("/users/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return userRepository.findById(Long.valueOf(jwt.getSubject()))
				.map(UserResponse::from)
				.orElseThrow(() -> ApiException.notFound("User not found"));
	}

	/** AGENT and ADMIN only (see SecurityConfig). Used later for ticket assignment. */
	@GetMapping("/agents")
	public List<UserResponse> agents() {
		return userRepository.findByRoleAndEnabledTrueOrderByName(Role.AGENT).stream()
				.map(UserResponse::from)
				.toList();
	}

}
