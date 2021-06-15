package io.eventuate.examples.tram.ordersandcustomers.customers.domain.events;

public class CustomerCreditReleasedEvent extends AbstractCustomerOrderEvent {

  public CustomerCreditReleasedEvent() {
  }

  public CustomerCreditReleasedEvent(String orderId) {
    super(orderId);
  }
}
