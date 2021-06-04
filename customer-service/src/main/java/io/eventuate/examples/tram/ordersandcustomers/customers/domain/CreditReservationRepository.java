package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CreditReservationRepository extends ReactiveCrudRepository<CreditReservation, Long> {
  Flux<CreditReservation> findAllByCustomerId(Long customerId);
  Mono<Void> deleteByOrderId(String orderId);
}
