package io.eventuate.examples.tram.ordersandcustomers.orders.webapi;


import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderState;

public class GetOrderResponse {
  private String orderId;
  private OrderState orderState;

  public GetOrderResponse() {
  }

  public GetOrderResponse(String orderId, OrderState orderState) {
    this.orderId = orderId;
    this.orderState = orderState;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public OrderState getOrderState() {
    return orderState;
  }

  public void setOrderState(OrderState orderState) {
    this.orderState = orderState;
  }
}
