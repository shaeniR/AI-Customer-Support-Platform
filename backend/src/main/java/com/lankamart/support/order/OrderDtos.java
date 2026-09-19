package com.lankamart.support.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Response bodies for the order endpoints. Entities are never returned directly. */
public final class OrderDtos {

	private OrderDtos() {
	}

	public record OrderSummaryResponse(
			String orderNumber,
			OrderStatus status,
			PaymentStatus paymentStatus,
			BigDecimal totalAmount,
			LocalDate expectedDeliveryDate,
			Instant createdAt) {

		public static OrderSummaryResponse from(CustomerOrder order) {
			return new OrderSummaryResponse(order.getOrderNumber(), order.getStatus(), order.getPaymentStatus(),
					order.getTotalAmount(), order.getExpectedDeliveryDate(), order.getCreatedAt());
		}
	}

	public record OrderItemResponse(
			Long productId,
			String productName,
			int quantity,
			BigDecimal unitPrice,
			BigDecimal lineTotal) {

		public static OrderItemResponse from(OrderItem item) {
			return new OrderItemResponse(item.getProduct().getId(), item.getProduct().getName(), item.getQuantity(),
					item.getUnitPrice(), item.getLineTotal());
		}
	}

	public record OrderDetailResponse(
			String orderNumber,
			OrderStatus status,
			PaymentStatus paymentStatus,
			BigDecimal totalAmount,
			LocalDate expectedDeliveryDate,
			Instant createdAt,
			List<OrderItemResponse> items) {

		public static OrderDetailResponse from(CustomerOrder order) {
			return new OrderDetailResponse(order.getOrderNumber(), order.getStatus(), order.getPaymentStatus(),
					order.getTotalAmount(), order.getExpectedDeliveryDate(), order.getCreatedAt(),
					order.getItems().stream().map(OrderItemResponse::from).toList());
		}
	}

}
