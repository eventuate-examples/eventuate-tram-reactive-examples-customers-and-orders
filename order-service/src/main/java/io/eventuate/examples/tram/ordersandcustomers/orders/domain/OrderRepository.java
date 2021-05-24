package io.eventuate.examples.tram.ordersandcustomers.orders.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface OrderRepository  extends ReactiveCrudRepository<Order, Long> {
}