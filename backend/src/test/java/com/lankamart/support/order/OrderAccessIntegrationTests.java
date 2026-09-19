package com.lankamart.support.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.lankamart.support.TestcontainersConfiguration;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class OrderAccessIntegrationTests {

	private static final String CUSTOMER_A = "customer@lankamart.lk";
	private static final String CUSTOMER_B = "nimali@lankamart.lk";
	private static final String PASSWORD = "Demo@12345";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void customerSeesOnlyOwnOrders() throws Exception {
		List<String> aOrders = orderNumbers(token(CUSTOMER_A));
		List<String> bOrders = orderNumbers(token(CUSTOMER_B));

		assertThat(aOrders).isNotEmpty();
		assertThat(bOrders).isNotEmpty();
		assertThat(aOrders).doesNotContainAnyElementsOf(bOrders);
	}

	@Test
	void customerCanReadOwnOrderWithItems() throws Exception {
		String tokenA = token(CUSTOMER_A);
		String ownOrder = orderNumbers(tokenA).get(0);

		mockMvc.perform(get("/api/v1/orders/" + ownOrder).header("Authorization", "Bearer " + tokenA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderNumber").value(ownOrder))
				.andExpect(jsonPath("$.items[0].productName").exists());
	}

	@Test
	void customerACanNeverReadCustomerBsOrder() throws Exception {
		String tokenA = token(CUSTOMER_A);
		String bOrder = orderNumbers(token(CUSTOMER_B)).get(0);

		String otherCustomersOrder = mockMvc
				.perform(get("/api/v1/orders/" + bOrder).header("Authorization", "Bearer " + tokenA))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.detail").value("Order not found"))
				.andExpect(jsonPath("$.orderNumber").doesNotExist())
				.andReturn().getResponse().getContentAsString();

		String missingOrder = mockMvc
				.perform(get("/api/v1/orders/ORD9999").header("Authorization", "Bearer " + tokenA))
				.andExpect(status().isNotFound())
				.andReturn().getResponse().getContentAsString();

		// Same answer for "someone else's order" and "no such order": nothing is revealed
		assertThat(JsonPath.<String>read(otherCustomersOrder, "$.detail"))
				.isEqualTo(JsonPath.<String>read(missingOrder, "$.detail"));
	}

	@Test
	void agentCannotUseCustomerOrderEndpoints() throws Exception {
		mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + token("agent@lankamart.lk")))
				.andExpect(status().isForbidden());
	}

	@Test
	void ordersRequireLogin() throws Exception {
		mockMvc.perform(get("/api/v1/orders")).andExpect(status().isUnauthorized());
	}

	private List<String> orderNumbers(String token) throws Exception {
		String body = mockMvc.perform(get("/api/v1/orders").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$[*].orderNumber");
	}

	private String token(String email) throws Exception {
		String body = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email": "%s", "password": "%s"}
						""".formatted(email, PASSWORD)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

}
