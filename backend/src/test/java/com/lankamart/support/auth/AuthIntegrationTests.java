package com.lankamart.support.auth;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.lankamart.support.TestcontainersConfiguration;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class AuthIntegrationTests {

	private static final String DEMO_PASSWORD = "Demo@12345";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtEncoder jwtEncoder;

	// ---------- Register ----------

	@Test
	void registerCreatesCustomer() throws Exception {
		String email = uniqueEmail();
		register("New Customer", email, "password123")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.role").value("CUSTOMER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void registerWithDuplicateEmailReturns409() throws Exception {
		String email = uniqueEmail();
		register("First", email, "password123").andExpect(status().isCreated());

		register("Second", email.toUpperCase(), "password123")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.detail").value("An account with this email already exists"))
				.andExpect(jsonPath("$.instance").value("/api/v1/auth/register"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void registerWithShortPasswordReturns400() throws Exception {
		register("Short", uniqueEmail(), "1234567")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors.password").exists());
	}

	// ---------- Login ----------

	@Test
	void demoAccountsCanLogIn() throws Exception {
		login("customer@lankamart.lk", DEMO_PASSWORD).andExpect(status().isOk())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.accessToken", notNullValue()))
				.andExpect(jsonPath("$.refreshToken", notNullValue()))
				.andExpect(jsonPath("$.expiresInSeconds").value(900))
				.andExpect(jsonPath("$.user.role").value("CUSTOMER"));
		login("agent@lankamart.lk", DEMO_PASSWORD).andExpect(jsonPath("$.user.role").value("AGENT"));
		login("admin@lankamart.lk", DEMO_PASSWORD).andExpect(jsonPath("$.user.role").value("ADMIN"));
	}

	@Test
	void wrongPasswordReturns401WithoutTokens() throws Exception {
		login("customer@lankamart.lk", "wrong-password")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.accessToken").doesNotExist());
	}

	@Test
	void loginIsBlockedAfterFiveFailedAttempts() throws Exception {
		String email = uniqueEmail();
		register("Locked", email, "password123").andExpect(status().isCreated());

		for (int i = 0; i < 5; i++) {
			login(email, "wrong-password").andExpect(status().isUnauthorized());
		}

		// Even the correct password is rejected now
		login(email, "password123")
				.andExpect(status().isTooManyRequests())
				.andExpect(header().exists("Retry-After"))
				.andExpect(jsonPath("$.status").value(429))
				.andExpect(jsonPath("$.retryAfterSeconds").exists());
	}

	// ---------- Refresh and logout ----------

	@Test
	void refreshReturnsNewTokensAndOldRefreshTokenCannotBeReused() throws Exception {
		String refreshToken = JsonPath.read(loginBody("customer@lankamart.lk"), "$.refreshToken");

		refresh(refreshToken).andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", notNullValue()));

		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		String refreshToken = JsonPath.read(loginBody("customer@lankamart.lk"), "$.refreshToken");

		mockMvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON)
				.content(refreshJson(refreshToken)))
				.andExpect(status().isNoContent());

		refresh(refreshToken).andExpect(status().isUnauthorized());
	}

	// ---------- Role checks ----------

	@Test
	void customerCannotCallAgentOrAdminEndpoints() throws Exception {
		String token = accessToken("customer@lankamart.lk");

		mockMvc.perform(get("/api/v1/agents").header("Authorization", bearer(token)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.instance").value("/api/v1/agents"));
		mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(token)))
				.andExpect(status().isForbidden());
	}

	@Test
	void agentCanCallAgentEndpointsButNotAdminEndpoints() throws Exception {
		String token = accessToken("agent@lankamart.lk");

		mockMvc.perform(get("/api/v1/agents").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(token)))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanCallAdminAndAgentEndpoints() throws Exception {
		String token = accessToken("admin@lankamart.lk");

		mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/agents").header("Authorization", bearer(token)))
				.andExpect(status().isOk());
	}

	@Test
	void loggedInUserCanReadOwnProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(accessToken("customer@lankamart.lk"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("customer@lankamart.lk"));
	}

	// ---------- Token checks ----------

	@Test
	void missingTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.instance").value("/api/v1/users/me"));
	}

	@Test
	void invalidTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer("not-a-real-token")))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void expiredTokenReturns401() throws Exception {
		Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
		String expired = sign(jwtEncoder, twoHoursAgo, twoHoursAgo.plus(15, ChronoUnit.MINUTES));

		mockMvc.perform(get("/api/v1/users/me").header("Authorization", bearer(expired)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithAnotherSecretReturns401() throws Exception {
		byte[] otherSecret = "a-completely-different-secret-key-of-32-plus-chars".getBytes(StandardCharsets.UTF_8);
		JwtEncoder attackerEncoder = new NimbusJwtEncoder(
				new ImmutableSecret<>(new SecretKeySpec(otherSecret, "HmacSHA256")));
		Instant now = Instant.now();
		String forged = sign(attackerEncoder, now, now.plus(15, ChronoUnit.MINUTES));

		mockMvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(forged)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unknownEndpointReturns404InStandardFormat() throws Exception {
		mockMvc.perform(get("/api/v1/does-not-exist").header("Authorization", bearer(accessToken("customer@lankamart.lk"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	// ---------- Helpers ----------

	private ResultActions register(String name, String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name": "%s", "email": "%s", "password": "%s"}
						""".formatted(name, email, password)));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}
						""".formatted(email, password)));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
				.content(refreshJson(refreshToken)));
	}

	private String loginBody(String email) throws Exception {
		return login(email, DEMO_PASSWORD).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private String accessToken(String email) throws Exception {
		return JsonPath.read(loginBody(email), "$.accessToken");
	}

	/** An ADMIN token with the given times, signed by the given encoder. */
	private static String sign(JwtEncoder encoder, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(JwtService.ISSUER)
				.subject("1")
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.claim("roles", List.of("ADMIN"))
				.build();
		return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
	}

	private static String refreshJson(String refreshToken) {
		return """
				{"refreshToken": "%s"}
				""".formatted(refreshToken);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

}
