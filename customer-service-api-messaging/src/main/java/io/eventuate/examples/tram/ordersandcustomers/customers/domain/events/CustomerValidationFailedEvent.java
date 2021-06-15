package io.eventuate.examples.tram.ordersandcustomers.customers.domain.events;

public class CustomerValidationFailedEvent extends AbstractCustomerOrderEvent {

  public CustomerValidationFailedEvent(String orderId) {
    super(orderId);
  }

  public CustomerValidationFailedEvent() {
  }
}
