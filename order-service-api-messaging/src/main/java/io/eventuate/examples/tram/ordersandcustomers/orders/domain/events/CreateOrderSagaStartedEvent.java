package io.eventuate.examples.tram.ordersandcustomers.orders.domain.events;

import io.eventuate.tram.events.common.DomainEvent;

public class CreateOrderSagaStartedEvent implements DomainEvent {
  private OrderDetails orderDetails;

  public CreateOrderSagaStartedEvent() {
  }

  public CreateOrderSagaStartedEvent(OrderDetails orderDetails) {
    this.orderDetails = orderDetails;
  }

  public OrderDetails getOrderDetails() {
    return orderDetails;
  }

  public void setOrderDetails(OrderDetails orderDetails) {
    this.orderDetails = orderDetails;
  }
}
