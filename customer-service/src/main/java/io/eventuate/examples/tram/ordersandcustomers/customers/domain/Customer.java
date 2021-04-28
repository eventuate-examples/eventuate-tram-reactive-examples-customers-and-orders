package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreatedEvent;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

import static java.util.Collections.singletonList;

@Table("customer")
public class Customer {

  @Id
  private Long id;
  private String name;

  private BigDecimal creditLimit;

  private Long creationTime;

  @Version
  private Long version;

  public Customer() {
  }

  public Customer(String name, BigDecimal creditLimit) {
    this.name = name;
    this.creditLimit = creditLimit;
    this.creationTime = System.currentTimeMillis();
  }

  public static ResultWithEvents<Customer> create(String name, Money creditLimit) {
    Customer customer = new Customer(name, creditLimit.getAmount());
    return new ResultWithEvents<>(customer,
            singletonList(new CustomerCreatedEvent(customer.getName(), new Money(customer.getCreditLimit()))));
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public BigDecimal getCreditLimit() {
    return creditLimit;
  }
}
