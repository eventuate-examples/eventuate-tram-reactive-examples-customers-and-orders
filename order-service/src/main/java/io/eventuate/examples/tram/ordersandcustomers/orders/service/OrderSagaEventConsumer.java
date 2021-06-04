package io.eventuate.examples.tram.ordersandcustomers.orders.service;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;


public class OrderSagaEventConsumer {

  @Autowired
  private OrderService orderService;

  public ReactiveDomainEventHandlers domainEventHandlers() {
    return ReactiveDomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order")
            .onEvent(CreateOrderSagaStartedEvent.class, this::handlerOrderCreatedSagaStarted)
            .build();
  }

  private Mono<Void> handlerOrderCreatedSagaStarted(DomainEventEnvelope<CreateOrderSagaStartedEvent> domainEventEnvelope) {
    return orderService
            .createOrder(domainEventEnvelope.getAggregateId(), domainEventEnvelope.getEvent().getOrderDetails())
            .then();
  }
}
