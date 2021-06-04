package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCreatedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderRejectedEvent;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class OrderEventConsumer {
  private Logger logger = LoggerFactory.getLogger(getClass());

  private OrderServiceProxyController orderServiceProxyController;

  public OrderEventConsumer(OrderServiceProxyController orderServiceProxyController) {
    this.orderServiceProxyController = orderServiceProxyController;
  }

  public ReactiveDomainEventHandlers domainEventHandlers() {
    return ReactiveDomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order")
            .onEvent(OrderCreatedEvent.class, this::handleOrderCreatedEvent)
            .onEvent(OrderRejectedEvent.class, this::handleOrderRejectedEvent)
            .build();
  }

  public Mono<Void> handleOrderCreatedEvent(DomainEventEnvelope<OrderCreatedEvent> domainEventEnvelope) {
    return Mono.fromRunnable(() -> orderServiceProxyController.createOrderSagaComplete(domainEventEnvelope.getAggregateId()));
  }

  public Mono<Void> handleOrderRejectedEvent(DomainEventEnvelope<OrderRejectedEvent> domainEventEnvelope) {
    return Mono.fromRunnable(() -> orderServiceProxyController.createOrderSagaComplete(domainEventEnvelope.getAggregateId()));
  }
}
