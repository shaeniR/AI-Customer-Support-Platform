package com.lankamart.support.order;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.lankamart.support.user.User;
import com.lankamart.support.user.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates 12 demo orders for the 3 demo customers. Products come from Flyway (V4), but orders need
 * the customer IDs, and the demo users are only created at startup by DemoUserSeeder, so this runs after it.
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.seed-orders", havingValue = "true")
public class DemoOrderSeeder implements ApplicationRunner {

	private final CustomerOrderRepository orderRepository;
	private final ProductRepository productRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (orderRepository.count() > 0) {
			return;
		}

		// customer@lankamart.lk
		order("customer@lankamart.lk", "ORD1001", OrderStatus.DELIVERED, PaymentStatus.PAID, 40, -33,
				Map.of("Samsung Galaxy A55 5G", 1, "Anker PowerCore 20000mAh Power Bank", 1));
		order("customer@lankamart.lk", "ORD1002", OrderStatus.DELIVERED, PaymentStatus.REFUNDED, 25, -20,
				Map.of("JBL Tune 520BT Headphones", 1));
		order("customer@lankamart.lk", "ORD1003", OrderStatus.SHIPPED, PaymentStatus.PAID, 3, 2,
				Map.of("Lenovo IdeaPad Slim 3 Laptop", 1, "Logitech MX Master 3S Mouse", 1));
		order("customer@lankamart.lk", "ORD1004", OrderStatus.PLACED, PaymentStatus.DUPLICATE_CHARGE, 1, 5,
				Map.of("Philips Air Fryer HD9200", 1));
		order("customer@lankamart.lk", "ORD1005", OrderStatus.CANCELLED, PaymentStatus.REFUNDED, 12, null,
				Map.of("Nike Revolution 7 Running Shoes", 1));

		// nimali@lankamart.lk
		order("nimali@lankamart.lk", "ORD1006", OrderStatus.OUT_FOR_DELIVERY, PaymentStatus.PAID, 4, 0,
				Map.of("Samsung 43-inch Crystal UHD Smart TV", 1));
		order("nimali@lankamart.lk", "ORD1007", OrderStatus.DELIVERED, PaymentStatus.PAID, 30, -25,
				Map.of("Dilmah Premium Ceylon Tea 400g", 3, "Kist Mixed Fruit Jam 510g", 2,
						"Munchee Super Cream Cracker 490g", 4));
		order("nimali@lankamart.lk", "ORD1008", OrderStatus.PLACED, PaymentStatus.PENDING, 0, 6,
				Map.of("Singer Rice Cooker 1.8L", 1, "Abans Electric Kettle 1.7L", 1));
		order("nimali@lankamart.lk", "ORD1009", OrderStatus.SHIPPED, PaymentStatus.DUPLICATE_CHARGE, 2, 3,
				Map.of("Xiaomi Smart Band 8", 2));

		// kasun@lankamart.lk
		order("kasun@lankamart.lk", "ORD1010", OrderStatus.DELIVERED, PaymentStatus.PAID, 60, -55,
				Map.of("Apple iPhone 15 128GB", 1));
		order("kasun@lankamart.lk", "ORD1011", OrderStatus.SHIPPED, PaymentStatus.PAID, 2, 1,
				Map.of("Canon PIXMA G3010 Printer", 1, "TP-Link Archer C6 Wi-Fi Router", 1));
		order("kasun@lankamart.lk", "ORD1012", OrderStatus.CANCELLED, PaymentStatus.REFUNDED, 8, null,
				Map.of("Sony WH-1000XM5 Headphones", 1));

		log.info("Created {} demo orders", orderRepository.count());
	}

	/**
	 * @param daysAgo       when the order was placed
	 * @param deliveryInDays expected delivery relative to today (negative = in the past, null = none)
	 */
	private void order(String customerEmail, String orderNumber, OrderStatus status, PaymentStatus paymentStatus,
			int daysAgo, Integer deliveryInDays, Map<String, Integer> productQuantities) {
		User customer = userRepository.findByEmail(customerEmail)
				.orElseThrow(() -> new IllegalStateException("Demo customer missing: " + customerEmail));
		LocalDate expectedDelivery = deliveryInDays == null ? null : LocalDate.now().plusDays(deliveryInDays);
		Instant createdAt = Instant.now().minus(daysAgo, ChronoUnit.DAYS);

		CustomerOrder order = new CustomerOrder(customer, orderNumber, status, paymentStatus, expectedDelivery,
				createdAt);
		productQuantities.forEach((productName, quantity) -> order.addItem(product(productName), quantity));
		orderRepository.save(order);
	}

	private Product product(String name) {
		return productRepository.findByName(name)
				.orElseThrow(() -> new IllegalStateException("Seed product missing: " + name));
	}

}
