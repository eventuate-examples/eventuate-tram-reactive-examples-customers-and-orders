package io.eventuate.examples.tram.ordersandcustomers.orders.service;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepSucceededEvent;
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
            .forAggregateType(Order.class.getName())
            .onEvent(CreateOrderSagaStartedEvent.class, this::handlerOrderCreatedSagaStarted)
            .onEvent(CreateOrderSagaStepSucceededEvent.class, this::handleCreateOrderSagaStepSucceededEvent)
            .onEvent(CreateOrderSagaStepFailedEvent.class, this::handleCreateOrderSagaStepFailedEvent)
            .build();
  }

  private Mono<Void> handlerOrderCreatedSagaStarted(DomainEventEnvelope<CreateOrderSagaStartedEvent> domainEventEnvelope) {
    return orderService
            .createOrder(domainEventEnvelope.getAggregateId(), domainEventEnvelope.getEvent().getOrderDetails())
            .flatMap(notUsed -> Mono.empty());
  }

  private Mono<Void> handleCreateOrderSagaStepSucceededEvent(DomainEventEnvelope<CreateOrderSagaStepSucceededEvent> domainEventEnvelope) {
    if (domainEventEnvelope.getEvent().getService().equals("Customer Service")) {
      return orderService.approveOrder(domainEventEnvelope.getAggregateId());
    }

    return Mono.empty();
  }

  private Mono<Void> handleCreateOrderSagaStepFailedEvent(DomainEventEnvelope<CreateOrderSagaStepFailedEvent> domainEventEnvelope) {
    if (domainEventEnvelope.getEvent().getService().equals("Customer Service")) {
      return orderService.rejectOrder(domainEventEnvelope.getAggregateId());
    }

    return Mono.empty();
  }
}
