package com.lankamart.support.order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

	List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

	/**
	 * The ownership check is part of the query: an order of another customer is simply not found.
	 * Items and their products are loaded in the same query.
	 */
	@EntityGraph(attributePaths = { "items", "items.product" })
	Optional<CustomerOrder> findByOrderNumberAndCustomerId(String orderNumber, Long customerId);

}
