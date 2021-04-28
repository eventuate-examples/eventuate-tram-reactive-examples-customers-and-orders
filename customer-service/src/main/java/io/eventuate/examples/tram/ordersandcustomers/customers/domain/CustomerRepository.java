package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {
}
