package io.eventuate.examples.tram.ordersandcustomers.orders.service;

import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreditReservationFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreditReservedEvent;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerValidationFailedEvent;

import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;


public class CustomerEventConsumer {

  @Autowired
  private OrderService orderService;

  public ReactiveDomainEventHandlers domainEventHandlers() {
    return ReactiveDomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.tram.ordersandcustomers.customers.domain.Customer")
            .onEvent(CustomerCreditReservedEvent.class, this::handleCustomerCreditReservedEvent)
            .onEvent(CustomerCreditReservationFailedEvent.class, this::handleCustomerCreditReservationFailedEvent)
            .onEvent(CustomerValidationFailedEvent.class, this::handleCustomerValidationFailedEvent)
            .build();
  }

  private Mono<Void> handleCustomerCreditReservedEvent(DomainEventEnvelope<CustomerCreditReservedEvent> domainEventEnvelope) {
    return orderService.approveOrder(domainEventEnvelope.getEvent().getOrderId());
  }

  private Mono<Void> handleCustomerCreditReservationFailedEvent(DomainEventEnvelope<CustomerCreditReservationFailedEvent> domainEventEnvelope) {
    return orderService.rejectOrder(domainEventEnvelope.getEvent().getOrderId());
  }

  private Mono<Void> handleCustomerValidationFailedEvent(DomainEventEnvelope<CustomerValidationFailedEvent> domainEventEnvelope) {
    return orderService.rejectOrder(domainEventEnvelope.getEvent().getOrderId());
  }

}
