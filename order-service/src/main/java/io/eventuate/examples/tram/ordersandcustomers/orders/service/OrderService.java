package io.eventuate.examples.tram.ordersandcustomers.orders.service;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.OrderRepository;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.*;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

public class OrderService {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ReactiveDomainEventPublisher domainEventPublisher;
  private final OrderRepository orderRepository;
  private final TransactionalOperator transactionalOperator;

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

    logger.info("createOrder {}", orderId);

    return orderRepository
            .save(order)
            .flatMap(o -> {
                logger.info("createOrder saved {}", orderId);
                return domainEventPublisher
                        .aggregateType(Order.class)
                        .aggregateId(orderId)
                        .events(orderWithEvents.events)
                        .event(new CreateOrderSagaStepSucceededEvent("Order Service", orderDetails))
                        .publish()
                        .thenReturn(order);
            })
            .as(transactionalOperator::transactional);
  }

  public Mono<?> approveOrder(String orderId) {
    logger.info("approveOrder {}", orderId);
      return orderRepository.findById(orderId)
            .map(order -> {
              order.noteCreditReserved();
              return order;
            })
            .flatMap(orderRepository::save)
            .flatMap(order -> {
                logger.info("approveOrder saved {}", orderId);
                return domainEventPublisher
                        .aggregateType(Order.class)
                        .aggregateId(orderId)
                        .event(new OrderApprovedEvent(order.getOrderDetails()))
                        .event(new CreateOrderSagaCompletedEvent("Order Service", order.getOrderDetails()))
                        .publish();
            });
  }

    public Mono<?> rejectOrder(String orderId) {
    logger.info("rejectOrder {}", orderId);
        return orderRepository.findById(orderId).map(order -> {
          order.noteCreditReservationFailed();
          return order;
        })
        .flatMap(orderRepository::save)
        .flatMap(order -> {
            logger.info("rejectOrder saved {}", orderId);
            return domainEventPublisher
                    .aggregateType(Order.class)
                    .aggregateId(orderId)
                    .event(new OrderRejectedEvent(order.getOrderDetails()))
                    .event(new CreateOrderSagaStepFailedEvent("Order Service", order.getOrderDetails()))
                    .publish();
        });
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