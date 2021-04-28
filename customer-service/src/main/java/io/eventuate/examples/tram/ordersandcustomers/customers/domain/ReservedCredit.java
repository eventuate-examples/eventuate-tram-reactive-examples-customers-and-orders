package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("reserved_credit")
public class ReservedCredit {

  @Id
  private Long id;

  private Long customerId;

  private Long orderId;

  private BigDecimal reservation;

  public ReservedCredit() {
  }

  public ReservedCredit(Long customerId, Long orderId, BigDecimal reservation) {
    this.customerId = customerId;
    this.orderId = orderId;
    this.reservation = reservation;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public BigDecimal getReservation() {
    return reservation;
  }

  public void setReservation(BigDecimal reservation) {
    this.reservation = reservation;
  }
}
