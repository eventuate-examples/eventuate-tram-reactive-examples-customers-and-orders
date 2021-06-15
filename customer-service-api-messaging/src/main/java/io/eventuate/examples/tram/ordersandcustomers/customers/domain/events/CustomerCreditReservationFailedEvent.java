package io.eventuate.examples.tram.ordersandcustomers.customers.domain.events;

public class CustomerCreditReservationFailedEvent extends AbstractCustomerOrderEvent {

  public CustomerCreditReservationFailedEvent() {
  }

  public CustomerCreditReservationFailedEvent(String orderId) {
    super(orderId);
  }


}
