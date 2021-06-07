package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.common.IdGenerator;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Collections;

@RestController
public class OrderServiceProxyController {
  private ReactiveDomainEventPublisher domainEventPublisher;
  private TransactionalOperator transactionalOperator;
  private OrderEventConsumer orderEventConsumer;

  @Autowired
  public OrderServiceProxyController(ReactiveDomainEventPublisher domainEventPublisher,
                                     TransactionalOperator transactionalOperator,
                                     OrderEventConsumer orderEventConsumer) {
    this.domainEventPublisher = domainEventPublisher;
    this.transactionalOperator = transactionalOperator;
    this.orderEventConsumer = orderEventConsumer;
  }


  @RequestMapping(value = "/orders", method = RequestMethod.POST)
  public Mono<ResponseEntity<CreateOrderResponse>> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
    return Mono
            .just(IdGenerator.generateId())
            .flatMap(orderAndSagaId -> {
              orderEventConsumer.prepareCreateOrderResponse(orderAndSagaId);

              CreateOrderSagaStartedEvent createOrderSagaStartedEvent =
                      new CreateOrderSagaStartedEvent(new OrderDetails(createOrderRequest.getCustomerId(), createOrderRequest.getOrderTotal()));

              return domainEventPublisher
                      .publish("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order",
                              orderAndSagaId, Collections.singletonList(createOrderSagaStartedEvent))
                      .collectList()
                      .map(messages -> orderAndSagaId)
                      .as(transactionalOperator::transactional);
            })
            .flatMap(orderAndSagaId -> Mono.fromFuture(orderEventConsumer.getCreateOrderResponse(orderAndSagaId)));
  }

}
