package io.eventuate.examples.tram.ordersandcustomers.customers.domain.events;

public abstract class AbstractCustomerOrderEvent implements CustomerEvent {
  protected String orderId;

  protected AbstractCustomerOrderEvent(String orderId) {
    this.orderId = orderId;
  }

  protected AbstractCustomerOrderEvent() {
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }
}
