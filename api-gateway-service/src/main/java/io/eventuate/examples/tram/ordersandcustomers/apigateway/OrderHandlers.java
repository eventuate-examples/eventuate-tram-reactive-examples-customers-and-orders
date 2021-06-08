package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.common.IdGenerator;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Collections;

public class OrderHandlers {
  private ReactiveDomainEventPublisher domainEventPublisher;
  private TransactionalOperator transactionalOperator;
  private OrderEventConsumer orderEventConsumer;

  public OrderHandlers(ReactiveDomainEventPublisher domainEventPublisher,
                       TransactionalOperator transactionalOperator,
                       OrderEventConsumer orderEventConsumer) {
    this.domainEventPublisher = domainEventPublisher;
    this.transactionalOperator = transactionalOperator;
    this.orderEventConsumer = orderEventConsumer;
  }


  public Mono<ServerResponse> createOrder(ServerRequest serverRequest) {
    return serverRequest
            .bodyToMono(CreateOrderRequest.class)
            .flatMap(this::requestOrderCreation)
            .flatMap(this::finishOrderCreation);
  }

  private Mono<ServerResponse> finishOrderCreation(String orderAndSagaId) {
    return Mono
            .fromFuture(orderEventConsumer.getCreateOrderResponse(orderAndSagaId))
            .flatMap(orderState -> createServerResponse(orderAndSagaId, orderState));
  }

  private Mono<String> requestOrderCreation(CreateOrderRequest createOrderRequest) {
    String orderAndSagaId = IdGenerator.generateId();

    orderEventConsumer.prepareCreateOrderResponse(orderAndSagaId);

    CreateOrderSagaStartedEvent createOrderSagaStartedEvent =
            new CreateOrderSagaStartedEvent(new OrderDetails(createOrderRequest.getCustomerId(), createOrderRequest.getOrderTotal()));

    return domainEventPublisher
            .publish("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order",
                    orderAndSagaId, Collections.singletonList(createOrderSagaStartedEvent))
            .collectList()
            .map(messages -> orderAndSagaId)
            .as(transactionalOperator::transactional);
  }

  private Mono<ServerResponse> createServerResponse(String orderId, OrderState orderState) {
    CreateOrderResponse createOrderResponse = new CreateOrderResponse(orderId);

    if (orderState == OrderState.COMPLETE) {
      return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(createOrderResponse);
    } else {
      return ServerResponse.badRequest().contentType(MediaType.APPLICATION_JSON).bodyValue(createOrderResponse);
    }
  }

}
