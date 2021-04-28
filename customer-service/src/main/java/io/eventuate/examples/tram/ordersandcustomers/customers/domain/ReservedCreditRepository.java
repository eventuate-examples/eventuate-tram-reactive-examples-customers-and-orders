package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ReservedCreditRepository extends ReactiveCrudRepository<ReservedCredit, Long> {
  Flux<ReservedCredit> findAllByCustomerId(Long customerId);
  Mono<Void> deleteByOrderId(Long orderId);
}
