package com.lankamart.support.order;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lankamart.support.common.ApiException;
import com.lankamart.support.order.OrderDtos.OrderDetailResponse;
import com.lankamart.support.order.OrderDtos.OrderSummaryResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

	private final CustomerOrderRepository orderRepository;

	public List<OrderSummaryResponse> listForCustomer(Long customerId) {
		return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
				.map(OrderSummaryResponse::from)
				.toList();
	}

	/** Empty when the order does not exist OR belongs to someone else. Reused later by the AI order tool. */
	public Optional<OrderDetailResponse> findForCustomer(String orderNumber, Long customerId) {
		return orderRepository.findByOrderNumberAndCustomerId(normalize(orderNumber), customerId)
				.map(OrderDetailResponse::from);
	}

	/** Same 404 for "does not exist" and "not yours", so other customers' order numbers are not revealed. */
	public OrderDetailResponse getForCustomer(String orderNumber, Long customerId) {
		return findForCustomer(orderNumber, customerId)
				.orElseThrow(() -> ApiException.notFound("Order not found"));
	}

	private static String normalize(String orderNumber) {
		return orderNumber.trim().toUpperCase(Locale.ROOT);
	}

}
