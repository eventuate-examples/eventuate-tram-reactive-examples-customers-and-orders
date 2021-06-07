package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaCompletedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.tram.events.subscriber.DomainEventEnvelope;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlers;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventHandlersBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class OrderEventConsumer {
  private Logger logger = LoggerFactory.getLogger(getClass());

  private ConcurrentHashMap<String, CompletableFuture<ResponseEntity<CreateOrderResponse>>> sagaIdToCompletableFuture = new ConcurrentHashMap<>();

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

  public CompletableFuture<ResponseEntity<CreateOrderResponse>> getCreateOrderResponse(String createOrderSagaId) {
    return sagaIdToCompletableFuture.get(createOrderSagaId);
  }

  public void prepareCreateOrderResponse(String createOrderSagaId) {
    sagaIdToCompletableFuture.put(createOrderSagaId, new CompletableFuture<>());
  }


  private void completeOrderCreation(String orderId) {
    Optional
            .ofNullable(sagaIdToCompletableFuture.remove(orderId))
            .ifPresent(createOrderResponseCompletableFuture ->
              createOrderResponseCompletableFuture.complete(new ResponseEntity<>(new CreateOrderResponse(orderId), HttpStatus.OK)));
  }

  private void failOrderCreation(String orderId) {
    Optional
            .ofNullable(sagaIdToCompletableFuture.remove(orderId))
            .ifPresent(createOrderResponseCompletableFuture ->
                    createOrderResponseCompletableFuture.complete(new ResponseEntity<>(new CreateOrderResponse(orderId), HttpStatus.BAD_REQUEST)));
  }
}
