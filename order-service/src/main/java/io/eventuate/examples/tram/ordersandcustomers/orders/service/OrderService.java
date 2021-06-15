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
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.util.List;

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
                    .aggregateType(Order.class)
                    .aggregateId(orderId)
                    .events(orderWithEvents.events)
                    .event(new CreateOrderSagaStepSucceededEvent("Order Service", orderDetails))
                    .publish()
                    .thenReturn(order))
            .as(transactionalOperator::transactional);
  }

  public Mono<List<Message>> approveOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.noteCreditReserved();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(order ->
                    domainEventPublisher
                            .aggregateType(Order.class)
                            .aggregateId(orderId)
                            .event(new OrderApprovedEvent(order.getOrderDetails()))
                            .event(new CreateOrderSagaCompletedEvent("Order Service", order.getOrderDetails()))
                            .publish());
  }

  public Mono<List<Message>> rejectOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.noteCreditReservationFailed();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(order ->
                    domainEventPublisher
                            .aggregateType(Order.class)
                            .aggregateId(orderId)
                            .event(new OrderRejectedEvent(order.getOrderDetails()))
                            .event(new CreateOrderSagaStepFailedEvent("Order Service", order.getOrderDetails()))
                            .publish());
  }

  public Mono<Order> cancelOrder(String orderId) {
    return orderRepository
            .findById(orderId)
            .map(order -> {
              order.cancel();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(order -> domainEventPublisher.publish(Order.class, orderId, new OrderCancelledEvent(order.getOrderDetails())).thenReturn(order))
            .as(transactionalOperator::transactional);
  }
}