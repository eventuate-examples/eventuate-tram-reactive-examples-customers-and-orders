package io.eventuate.examples.tram.ordersandcustomers.orders.domain.events;

import io.eventuate.tram.events.common.DomainEvent;

public class CreateOrderSagaStepSucceededEvent implements DomainEvent {
  private String service;
  private OrderDetails orderDetails;

  public CreateOrderSagaStepSucceededEvent() {
  }

  public CreateOrderSagaStepSucceededEvent(String service, OrderDetails orderDetails) {
    this.service = service;
    this.orderDetails = orderDetails;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public OrderDetails getOrderDetails() {
    return orderDetails;
  }

  public void setOrderDetails(OrderDetails orderDetails) {
    this.orderDetails = orderDetails;
  }
}
