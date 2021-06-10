package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.common.IdGenerator;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

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
            .flatMap(createOrderRequest -> {
              String orderAndSagaId = IdGenerator.generateId();

              CreateOrderSagaStartedEvent createOrderSagaStartedEvent =
                      new CreateOrderSagaStartedEvent(new OrderDetails(createOrderRequest.getCustomerId(), createOrderRequest.getOrderTotal()));

              Mono<OrderState> response = orderEventConsumer.prepareCreateOrderResponse(orderAndSagaId);

              return domainEventPublisher
                      .publish("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order",
                              orderAndSagaId, createOrderSagaStartedEvent)
                      .as(transactionalOperator::transactional)
                      .flatMap(messages -> response)
                      .flatMap(orderState -> createServerResponse(orderAndSagaId, orderState));
            });
  }


  private Mono<ServerResponse> createServerResponse(String orderId, OrderState orderState) {
    CreateOrderResponse createOrderResponse = new CreateOrderResponse(orderId);

    HttpStatus status;

    switch (orderState) {
      case FAILED: {
        status = HttpStatus.BAD_REQUEST;
        break;
      }
      case COMPLETE: {
        status = HttpStatus.OK;
        break;
      }
      case TIMEOUT: {
        status = HttpStatus.ACCEPTED;
        break;
      }
      default: {
        throw new IllegalStateException(); //Should not be here
      }
    }

    return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).bodyValue(createOrderResponse);
  }
}
