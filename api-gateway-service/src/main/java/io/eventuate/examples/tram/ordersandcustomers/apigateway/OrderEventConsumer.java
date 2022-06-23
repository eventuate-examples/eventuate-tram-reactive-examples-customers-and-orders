package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaCompletedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepFailedEvent;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static reactor.core.publisher.Mono.just;

public class OrderEventConsumer {
  private Logger logger = LoggerFactory.getLogger(getClass());

  private ConcurrentHashMap<String, CompletableFuture<OrderState>> sagaIdToCompletableFuture = new ConcurrentHashMap<>();

  private Duration timeout;

  public OrderEventConsumer(Duration timeout) {
    this.timeout = timeout;
  }

  public ReactiveDomainEventHandlers domainEventHandlers() {
    return ReactiveDomainEventHandlersBuilder
            .forAggregateType("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order")
            .onEvent(CreateOrderSagaCompletedEvent.class, this::handleCreateOrderSagaCompletedEvent)
            .onEvent(CreateOrderSagaStepFailedEvent.class, this::handleCreateOrderSagaStepFailedEvent)
            .build();
  }

  public Mono<Void> handleCreateOrderSagaCompletedEvent(DomainEventEnvelope<CreateOrderSagaCompletedEvent> domainEventEnvelope) {
    return Mono.fromRunnable(() -> completeOrderCreation(domainEventEnvelope.getAggregateId()));
  }

  public Mono<Void> handleCreateOrderSagaStepFailedEvent(DomainEventEnvelope<CreateOrderSagaStepFailedEvent> domainEventEnvelope) {
    return Mono.fromRunnable(() -> failOrderCreation(domainEventEnvelope.getAggregateId()));
  }

  public Mono<OrderState> prepareCreateOrderResponse(String createOrderSagaId) {
    CompletableFuture<OrderState> future = new CompletableFuture<>();

    sagaIdToCompletableFuture.put(createOrderSagaId, future);

    return Mono
            .fromFuture(future)
            .timeout(timeout)
            .onErrorResume(TimeoutException.class::isInstance, e -> {
              logger.info("createOrderSaga timeout");
              return just(OrderState.TIMEOUT);
            });
  }

  private void completeOrderCreation(String orderId) {
    handlerOrderCreation(orderId, OrderState.COMPLETE);
  }

  private void failOrderCreation(String orderId) {
    handlerOrderCreation(orderId, OrderState.FAILED);
  }

  private void handlerOrderCreation(String orderId, OrderState orderState) {
    Optional
            .ofNullable(sagaIdToCompletableFuture.remove(orderId))
            .ifPresent(createOrderResponse ->
                    createOrderResponse.complete(orderState));
  }
}
