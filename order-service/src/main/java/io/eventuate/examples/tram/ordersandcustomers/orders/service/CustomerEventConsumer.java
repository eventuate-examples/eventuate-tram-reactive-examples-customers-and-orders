package io.eventuate.examples.tram.ordersandcustomers.orders.service;

import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerValidationFailedEvent;

import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.util.List;


public class CustomerEventConsumer {

  @Autowired
  private OrderService orderService;

  public ReactiveDomainEventHandlers domainEventHandlers() {
    return ReactiveDomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.tram.ordersandcustomers.customers.domain.Customer")
            .onEvent(CustomerValidationFailedEvent.class, this::handleCustomerValidationFailedEvent)
            .build();
  }

  private Mono<List<Message>> handleCustomerValidationFailedEvent(DomainEventEnvelope<CustomerValidationFailedEvent> domainEventEnvelope) {
    return orderService.rejectOrder(domainEventEnvelope.getEvent().getOrderId());
  }

}
