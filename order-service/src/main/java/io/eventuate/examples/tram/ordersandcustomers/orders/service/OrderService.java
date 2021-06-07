package io.eventuate.examples.tram.ordersandcustomers.orders.service;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaCompletedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepSucceededEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderApprovedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCancelledEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderRejectedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.OrderRepository;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static java.util.Collections.singletonList;

//TODO: remove duplicated code
public class OrderService {

  private final ReactiveDomainEventPublisher domainEventPublisher;
  private final OrderRepository orderRepository;
  private TransactionalOperator transactionalOperator;

  public OrderService(ReactiveDomainEventPublisher domainEventPublisher,
                      OrderRepository orderRepository,
                      TransactionalOperator transactionalOperator) {
    this.domainEventPublisher = domainEventPublisher;
    this.orderRepository = orderRepository;
    this.transactionalOperator = transactionalOperator;
  }

  public Mono<Order> createOrder(String orderId, OrderDetails orderDetails) {
    ResultWithEvents<Order> orderWithEvents = Order.createOrder(orderId, orderDetails);
    Order order = orderWithEvents.result;

    return orderRepository
            .save(order)
            .flatMap(o -> domainEventPublisher
                    .publish(Order.class, order.getId(), orderWithEvents.events)
                    .collectList()
                    .thenReturn(o))
            .flatMap(o -> {
              CreateOrderSagaStepSucceededEvent createOrderSagaStepSucceededEvent =
                      new CreateOrderSagaStepSucceededEvent("Order Service", orderDetails);

              return domainEventPublisher.publish(Order.class,
                      orderId,
                      Collections.singletonList(createOrderSagaStepSucceededEvent))
                      .collectList().flatMap(notUsed -> Mono.just(o));
            })
            .as(transactionalOperator::transactional);
  }

  public Mono<Void> approveOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.noteCreditReserved();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(o ->
                    domainEventPublisher
                            .publish(Order.class, orderId, singletonList(new OrderApprovedEvent(o.getOrderDetails())))
                            .collectList()
                            .flatMap(notUsed ->
                              domainEventPublisher.publish(Order.class,
                                      orderId,
                                      Collections.singletonList(new CreateOrderSagaCompletedEvent("Order Service", o.getOrderDetails())))
                                      .collectList()
                            )
            )
            .then();
  }

  public Mono<Void> rejectOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.noteCreditReservationFailed();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(o ->
                    domainEventPublisher
                            .publish(Order.class, orderId, singletonList(new OrderRejectedEvent(o.getOrderDetails())))
                            .collectList()
                            .flatMap(notUsed ->
                                    domainEventPublisher.publish(Order.class,
                                            orderId,
                                            Collections.singletonList(new CreateOrderSagaStepFailedEvent("Order Service", o.getOrderDetails())))
                                            .collectList()
                            )
            )
            .then();
  }

  public Mono<Order> cancelOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.cancel();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(o -> domainEventPublisher.publish(Order.class, orderId, singletonList(new OrderCancelledEvent(o.getOrderDetails()))).collectList().thenReturn(o))
            .as(transactionalOperator::transactional);
  }
}