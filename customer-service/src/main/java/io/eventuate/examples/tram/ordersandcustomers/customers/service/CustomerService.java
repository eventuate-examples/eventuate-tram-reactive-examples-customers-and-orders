package io.eventuate.examples.tram.ordersandcustomers.customers.service;

import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.Customer;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CustomerRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CreditReservation;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CreditReservationRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreditReservationFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerCreditReservedEvent;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.events.CustomerValidationFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepFailedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStepSucceededEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.tram.events.common.DomainEvent;
import io.eventuate.tram.events.publisher.ResultWithEvents;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

public class CustomerService {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private CustomerRepository customerRepository;

  private CreditReservationRepository creditReservationRepository;

  private ReactiveDomainEventPublisher domainEventPublisher;

  private TransactionalOperator transactionalOperator;

  public CustomerService(CustomerRepository customerRepository,
                         CreditReservationRepository creditReservationRepository,
                         ReactiveDomainEventPublisher domainEventPublisher,
                         TransactionalOperator transactionalOperator) {
    this.customerRepository = customerRepository;
    this.creditReservationRepository = creditReservationRepository;
    this.domainEventPublisher = domainEventPublisher;
    this.transactionalOperator = transactionalOperator;
  }

  public Mono<Customer> createCustomer(String name, Money creditLimit) {
    ResultWithEvents<Customer> customerWithEvents = Customer.create(name, creditLimit);

    return customerRepository
            .save(customerWithEvents.result)
            .flatMap(customer ->
                    domainEventPublisher
                            .publish(Customer.class, customer.getId(), customerWithEvents.events)
                            .thenReturn(customer))
            .as(transactionalOperator::transactional);
  }

  public Mono<List<Message>> reserveCredit(String orderId, long customerId, Money orderTotal) {

    Mono<Customer> possibleCustomer = customerRepository.findById(customerId);

    return possibleCustomer
            .flatMap(customer -> creditReservationRepository
                    .findAllByCustomerId(customerId)
                    .collectList()
                    .flatMap(creditReservations -> handleCreditReservation(creditReservations, customer, orderId, customerId, orderTotal)))
            .switchIfEmpty(handleNotExistingCustomer(orderId, customerId))
            .as(transactionalOperator::transactional)
            .doOnError(throwable -> logger.error("credit reservation failed", throwable));
  }

  public Mono<Void> releaseCredit(String orderId, long customerId) {
      return creditReservationRepository
              .deleteByOrderId(orderId).as(transactionalOperator::transactional)
              .doOnError(throwable -> logger.error("credit releasing failed", throwable));
  }

  private Mono<List<Message>> handleCreditReservation(List<CreditReservation> creditReservations, Customer customer, String orderId, long customerId, Money orderTotal) {
    BigDecimal currentReservations =
            creditReservations.stream().map(CreditReservation::getReservation).reduce(BigDecimal.ZERO, BigDecimal::add);

    if (currentReservations.add(orderTotal.getAmount()).compareTo(customer.getCreditLimit()) <= 0) {
      logger.info("reserving credit (orderId: {}, customerId: {})", orderId, customerId);

      CustomerCreditReservedEvent customerCreditReservedEvent =
              new CustomerCreditReservedEvent(orderId);

      CreateOrderSagaStepSucceededEvent createOrderSagaStepSucceededEvent =
              new CreateOrderSagaStepSucceededEvent("Customer Service", new OrderDetails(customerId, orderTotal));

      return creditReservationRepository
              .save(new CreditReservation(customerId, orderId, orderTotal.getAmount()))
              .then(publishCreditReservationEvents(customerId, customerCreditReservedEvent, orderId, createOrderSagaStepSucceededEvent));
    } else {
      logger.info("handling credit reservation failure (orderId: {}, customerId: {})", orderId, customerId);

      CustomerCreditReservationFailedEvent customerCreditReservationFailedEvent =
              new CustomerCreditReservationFailedEvent(orderId);

      CreateOrderSagaStepFailedEvent createOrderSagaStepFailedEvent =
                        new CreateOrderSagaStepFailedEvent("Customer Service", new OrderDetails(customerId, orderTotal));

      return publishCreditReservationEvents(customerId, customerCreditReservationFailedEvent, orderId, createOrderSagaStepFailedEvent);
    }
  }

  private Mono<List<Message>> publishCreditReservationEvents(Long customerId, DomainEvent customerEvent, String orderId, DomainEvent orderSagaEvent) {
    return domainEventPublisher
                    .aggregateType(Customer.class).aggregateId(customerId).event(customerEvent)
                    .aggregateType("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order").aggregateId(orderId).event(orderSagaEvent)
                    .publish();
  }

  private Mono<List<Message>> handleNotExistingCustomer(String orderId, long customerId) {
    return Mono.defer(() ->
      domainEventPublisher
                      .aggregateType(Customer.class)
                      .aggregateId(customerId)
                      .event(new CustomerValidationFailedEvent(orderId))
                      .publish());
  }
}
