package io.eventuate.examples.tram.ordersandcustomers.orders.domain;


import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCreatedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderState;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

import static java.util.Collections.singletonList;

@Table("ordert")
public class Order {

  @Id
  private Long id;

  private String state;
  private Long customerId;
  private BigDecimal orderTotal;

  public Order() {
  }

  public Order(OrderDetails orderDetails) {
    this.customerId = orderDetails.getCustomerId();
    this.orderTotal = orderDetails.getOrderTotal().getAmount();
    this.state = OrderState.PENDING.name();
  }

  public static ResultWithEvents<Order> createOrder(OrderDetails orderDetails) {
    Order order = new Order(orderDetails);
    OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(orderDetails);
    return new ResultWithEvents<>(order, singletonList(orderCreatedEvent));
  }

  public Long getId() {
    return id;
  }

  public void noteCreditReserved() {
    this.state = OrderState.APPROVED.name();
  }

  public void noteCreditReservationFailed() {
    this.state = OrderState.REJECTED.name();
  }

  public OrderState getState() {
    return OrderState.valueOf(state);
  }

  public OrderDetails getOrderDetails() {
    return new OrderDetails(customerId, new Money(orderTotal));
  }

  public void cancel() {
    switch (OrderState.valueOf(state)) {
      case PENDING:
        throw new PendingOrderCantBeCancelledException();
      case APPROVED:
        this.state = OrderState.CANCELLED.name();
        return;
      default:
        throw new UnsupportedOperationException("Can't cancel in this state: " + state);
    }
  }
}
