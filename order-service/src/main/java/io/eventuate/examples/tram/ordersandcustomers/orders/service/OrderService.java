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
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;

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
            .flatMap(o -> publishOrderEvents(order.getId(),
                    orderWithEvents.events,
                    Collections.singletonList(new CreateOrderSagaStepSucceededEvent("Order Service", orderDetails)))
                    .map(notUsed -> o))
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
            .flatMap(order -> publishOrderEvents(order.getId(),
                    new OrderApprovedEvent(order.getOrderDetails()),
                    new CreateOrderSagaCompletedEvent("Order Service", order.getOrderDetails())))
            .flatMap(notUsed -> Mono.empty());
  }

  public Mono<Void> rejectOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.noteCreditReservationFailed();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(order -> publishOrderEvents(order.getId(),
                    new OrderRejectedEvent(order.getOrderDetails()),
                    new CreateOrderSagaStepFailedEvent("Order Service", order.getOrderDetails())))
            .flatMap(notUsed -> Mono.empty());
  }

  public Mono<Order> cancelOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.cancel();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(order -> domainEventPublisher.publish(Order.class, orderId, singletonList(new OrderCancelledEvent(order.getOrderDetails())))
                    .collectList()
                    .map(notUsed -> order))
            .as(transactionalOperator::transactional);
  }

  private Mono<List<Message>> publishOrderEvents(String orderId, DomainEvent orderEvent, DomainEvent orderSagaEvent) {
    return publishOrderEvents(orderId, Collections.singletonList(orderEvent), Collections.singletonList(orderSagaEvent));
  }

  private Mono<List<Message>> publishOrderEvents(String orderId, List<DomainEvent> orderEvents, List<DomainEvent> orderSagaEvents) {
    return domainEventPublisher
            .publish(Order.class, orderId, orderEvents)
            .collectList()
            .flatMap(notUsed ->
                    domainEventPublisher.publish(Order.class, orderId, orderSagaEvents).collectList());
  }
}