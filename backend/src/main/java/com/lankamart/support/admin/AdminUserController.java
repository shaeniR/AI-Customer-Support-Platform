package com.lankamart.support.admin;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lankamart.support.user.UserRepository;
import com.lankamart.support.user.UserResponse;

import lombok.RequiredArgsConstructor;

/** ADMIN only (see SecurityConfig). */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserRepository userRepository;

	@GetMapping
	public List<UserResponse> listUsers() {
		return userRepository.findAll(Sort.by("id")).stream()
				.map(UserResponse::from)
				.toList();
	}

}
