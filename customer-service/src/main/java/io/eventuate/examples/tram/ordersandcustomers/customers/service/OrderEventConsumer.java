package io.eventuate.examples.tram.ordersandcustomers.customers.service;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCancelledEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCreatedEvent;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class OrderEventConsumer {
  private Logger logger = LoggerFactory.getLogger(getClass());

  private CustomerService customerService;

  public OrderEventConsumer(CustomerService customerService) {
    this.customerService = customerService;
  }

  public ReactiveDomainEventHandlers domainEventHandlers() {
    return ReactiveDomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order")
            .onEvent(OrderCreatedEvent.class, this::handleOrderCreatedEvent)
            .onEvent(OrderCancelledEvent.class, this::handleOrderCancelledEvent)
            .build();
  }

  public Mono<Void> handleOrderCreatedEvent(DomainEventEnvelope<OrderCreatedEvent> domainEventEnvelope) {
    OrderCreatedEvent event = domainEventEnvelope.getEvent();
    return customerService.reserveCredit(Long.parseLong(domainEventEnvelope.getAggregateId()),
            event.getOrderDetails().getCustomerId(), event.getOrderDetails().getOrderTotal());
  }

  public Mono<Void> handleOrderCancelledEvent(DomainEventEnvelope<OrderCancelledEvent> domainEventEnvelope) {
    return customerService.releaseCredit(Long.parseLong(domainEventEnvelope.getAggregateId()),
            domainEventEnvelope.getEvent().getOrderDetails().getCustomerId());
  }

}
