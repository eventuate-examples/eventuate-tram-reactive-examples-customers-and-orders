package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.common.IdGenerator;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class OrderServiceProxyController {
  private ReactiveDomainEventPublisher domainEventPublisher;
  private TransactionalOperator transactionalOperator;


  @Autowired
  public OrderServiceProxyController(ReactiveDomainEventPublisher domainEventPublisher, TransactionalOperator transactionalOperator) {
    this.domainEventPublisher = domainEventPublisher;
    this.transactionalOperator = transactionalOperator;
  }

  private ConcurrentHashMap<String, CompletableFuture<CreateOrderResponse>> sagaIdToCompletableFuture = new ConcurrentHashMap<>();

  @RequestMapping(value = "/orders", method = RequestMethod.POST)
  public Mono<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
    String orderAndSagaId = IdGenerator.generateId();

    CreateOrderSagaStartedEvent createOrderSagaStartedEvent =
            new CreateOrderSagaStartedEvent(new OrderDetails(createOrderRequest.getCustomerId(), createOrderRequest.getOrderTotal()));

    CompletableFuture<CreateOrderResponse> response = new CompletableFuture<>();

    sagaIdToCompletableFuture.put(orderAndSagaId, response);

    return domainEventPublisher
            .publish("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order", orderAndSagaId, Collections.singletonList(createOrderSagaStartedEvent))
            .as(transactionalOperator::transactional)
            .then(Mono.fromFuture(response));
  }

  public void createOrderSagaComplete(String orderId) {
    Optional
            .ofNullable(sagaIdToCompletableFuture.remove(orderId))
            .ifPresent(createOrderResponseCompletableFuture -> {
              createOrderResponseCompletableFuture.complete(new CreateOrderResponse(orderId));
            });
  }
}
