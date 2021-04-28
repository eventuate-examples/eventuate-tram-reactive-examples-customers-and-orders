package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("credit_reservation")
public class CreditReservation {

  @Id
  private Long id;

  private Long customerId;

  private Long orderId;

  private BigDecimal reservation;

  public CreditReservation() {
  }

  public CreditReservation(Long customerId, Long orderId, BigDecimal reservation) {
    this.customerId = customerId;
    this.orderId = orderId;
    this.reservation = reservation;
  }

  public BigDecimal getReservation() {
    return reservation;
  }
}
