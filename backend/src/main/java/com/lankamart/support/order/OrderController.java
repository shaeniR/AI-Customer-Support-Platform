package com.lankamart.support.order;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lankamart.support.order.OrderDtos.OrderDetailResponse;
import com.lankamart.support.order.OrderDtos.OrderSummaryResponse;

import lombok.RequiredArgsConstructor;

/** CUSTOMER only (see SecurityConfig). The customer ID always comes from the token, never from the request. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping
	public List<OrderSummaryResponse> myOrders(@AuthenticationPrincipal Jwt jwt) {
		return orderService.listForCustomer(customerId(jwt));
	}

	@GetMapping("/{orderNumber}")
	public OrderDetailResponse myOrder(@PathVariable String orderNumber, @AuthenticationPrincipal Jwt jwt) {
		return orderService.getForCustomer(orderNumber, customerId(jwt));
	}

	private static Long customerId(Jwt jwt) {
		return Long.valueOf(jwt.getSubject());
	}

}
