package com.lankamart.support.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.lankamart.support.user.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Named CustomerOrder (not Order) to avoid clashing with SQL ORDER BY and Spring's @Order. */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class CustomerOrder {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private User customer;

	@Column(name = "order_number", nullable = false, unique = true, length = 20)
	private String orderNumber;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(nullable = false, length = 30)
	private OrderStatus status;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "payment_status", nullable = false, length = 30)
	private PaymentStatus paymentStatus;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount = BigDecimal.ZERO;

	@Column(name = "expected_delivery_date")
	private LocalDate expectedDeliveryDate;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItem> items = new ArrayList<>();

	public CustomerOrder(User customer, String orderNumber, OrderStatus status, PaymentStatus paymentStatus,
			LocalDate expectedDeliveryDate, Instant createdAt) {
		this.customer = customer;
		this.orderNumber = orderNumber;
		this.status = status;
		this.paymentStatus = paymentStatus;
		this.expectedDeliveryDate = expectedDeliveryDate;
		this.createdAt = createdAt;
	}

	/** Adds a line using the product's current price and keeps the total correct. */
	public void addItem(Product product, int quantity) {
		OrderItem item = new OrderItem(this, product, quantity, product.getPrice());
		items.add(item);
		totalAmount = totalAmount.add(item.getLineTotal());
	}

}
