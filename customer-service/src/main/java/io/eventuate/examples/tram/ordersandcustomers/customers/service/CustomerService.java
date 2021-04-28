package io.eventuate.examples.tram.ordersandcustomers.customers.service;

import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.Customer;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CustomerRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.ReservedCredit;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.ReservedCreditRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreditReservationFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreditReservedEvent;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerValidationFailedEvent;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class CustomerService {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private CustomerRepository customerRepository;

  private ReservedCreditRepository reservedCreditRepository;

  private ReactiveDomainEventPublisher domainEventPublisher;

  private TransactionalOperator transactionalOperator;

  public CustomerService(CustomerRepository customerRepository,
                         ReservedCreditRepository reservedCreditRepository,
                         ReactiveDomainEventPublisher domainEventPublisher,
                         TransactionalOperator transactionalOperator) {
    this.customerRepository = customerRepository;
    this.reservedCreditRepository = reservedCreditRepository;
    this.domainEventPublisher = domainEventPublisher;
    this.transactionalOperator = transactionalOperator;
  }

  public Mono<Customer> createCustomer(String name, Money creditLimit) {
    ResultWithEvents<Customer> customerWithEvents = Customer.create(name, creditLimit);

    return customerRepository
            .save(customerWithEvents.result)
            .flatMap(customer -> domainEventPublisher.publish(Customer.class, customer.getId(), customerWithEvents.events).collectList().map(messages -> customer))
            .as(transactionalOperator::transactional);
  }

  public Mono<Void> reserveCredit(long orderId, long customerId, Money orderTotal) {

    Mono<Customer> possibleCustomer = customerRepository.findById(customerId);

    return possibleCustomer
            .flatMap(customer -> reservedCreditRepository
                    .findAllByCustomerId(customerId)
                    .collectList()
                    .flatMap(reservedCredits -> handleCreditReservation(reservedCredits, customer, orderId, customerId, orderTotal)))
            .switchIfEmpty(handleNotExistingCustomer(orderId, customerId))
            .as(transactionalOperator::transactional)
            .doOnError(throwable -> logger.error("credit reservation failed", throwable))
            .then();
  }

  public Mono<Void> releaseCredit(long orderId, long customerId) {
      return reservedCreditRepository
              .deleteByOrderId(orderId).as(transactionalOperator::transactional)
              .doOnError(throwable -> logger.error("credit releasing failed", throwable))
              .then();
  }

  private Mono<List<Message>> handleCreditReservation(List<ReservedCredit> reservedCredits, Customer customer, long orderId, long customerId, Money orderTotal) {
    BigDecimal currentReservations =
            reservedCredits.stream().map(ReservedCredit::getReservation).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (currentReservations.add(orderTotal.getAmount()).compareTo(customer.getCreditLimit()) <= 0) {
      logger.info("reserving credit (orderId: {}, customerId: {})", orderId, customerId);

      CustomerCreditReservedEvent customerCreditReservedEvent =
              new CustomerCreditReservedEvent(orderId);

      Mono<ReservedCredit> reservedCredit =
              reservedCreditRepository.save(new ReservedCredit(customerId, orderId, orderTotal.getAmount()));

      return reservedCredit.flatMap(rc ->
              domainEventPublisher
                      .publish(Customer.class,
                              customer.getId(),
                              Collections.singletonList(customerCreditReservedEvent))
                      .collectList());
    } else {
      logger.info("handling credit reservation failure (orderId: {}, customerId: {})", orderId, customerId);

      CustomerCreditReservationFailedEvent customerCreditReservationFailedEvent =
              new CustomerCreditReservationFailedEvent(orderId);

      return domainEventPublisher.publish(Customer.class,
              customer.getId(),
              Collections.singletonList(customerCreditReservationFailedEvent))
              .collectList();
    }
  }

  private Mono<List<Message>> handleNotExistingCustomer(long orderId, long customerId) {
    return Mono.defer(() ->
      domainEventPublisher
              .publish(Customer.class,
                      customerId,
                      Collections.singletonList(new CustomerValidationFailedEvent(orderId)))
              .collectList());
  }
}
