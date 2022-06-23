package io.eventuate.examples.tram.ordersandcustomers.customers.service;

import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CreditReservationRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.Customer;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CustomerRepository;
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

  public Mono<?> reserveCredit(long customerId, String orderId, Money orderTotal) {

    Mono<Customer> possibleCustomer = customerRepository.findById(customerId);

    logger.info("Reserving credit for {}", customerId);

    return possibleCustomer
            .flatMap(customer -> reserveCredit(customer, orderId, orderTotal))
            .switchIfEmpty(handleNotExistingCustomer(orderId, customerId, orderTotal))
            .as(transactionalOperator::transactional)
            .doOnError(throwable -> logger.error("credit reservation failed", throwable));
  }

  private Mono<Object> reserveCredit(Customer customer, String orderId, Money orderTotal) {
    // Save the customer even though it hasn't changed to ensure serialized updates
    Long customerId = customer.getId();
    return creditReservationRepository
            .findAllByCustomerId(customerId)
            .collectList()
            .flatMap(creditReservations -> customer.attemptToReserveCredit(creditReservations, orderId, orderTotal)
                        .map(creditReservation -> creditReservationRepository
                                .save(creditReservation)
                                .then(publishReservationSucceededEvents(orderId, customerId, orderTotal)))
                        .orElseGet(() -> publishReservationFailedEvents(orderId, customerId, orderTotal)))
            .flatMap(ignored -> customerRepository.save(customer));
  }

  public Mono<Object> releaseCredit(long customerId, String orderId) {
      // Find and save the customer even though it hasn't changed to ensure serialized updates
      logger.info("releaseCredit credit for {}", customerId);
      return customerRepository.findById(customerId)
                      .flatMap(customer ->
              creditReservationRepository
              .deleteByOrderId(orderId).as(transactionalOperator::transactional)
              .flatMap(ignored -> customerRepository.save(customer)));
  }

  private Mono<List<Message>> publishReservationFailedEvents(String orderId, long customerId, Money orderTotal) {
    logger.info("handling credit reservation failure (orderId: {}, customerId: {})", orderId, customerId);

    CustomerCreditReservationFailedEvent reservationFailedEvent =
            new CustomerCreditReservationFailedEvent(orderId);

    CreateOrderSagaStepFailedEvent stepFailedEvent =
            new CreateOrderSagaStepFailedEvent("Customer Service", new OrderDetails(customerId, orderTotal));

    return publishCreditReservationEvents(customerId, reservationFailedEvent, orderId, stepFailedEvent);
  }

  private Mono<List<Message>> publishReservationSucceededEvents(String orderId, long customerId, Money orderTotal) {
    logger.info("reserved credit (orderId: {}, customerId: {})", orderId, customerId);

    CustomerCreditReservedEvent customerCreditReservedEvent =
            new CustomerCreditReservedEvent(orderId);

    CreateOrderSagaStepSucceededEvent createOrderSagaStepSucceededEvent =
            new CreateOrderSagaStepSucceededEvent("Customer Service", new OrderDetails(customerId, orderTotal));

    return publishCreditReservationEvents(customerId, customerCreditReservedEvent, orderId, createOrderSagaStepSucceededEvent);
  }

  private Mono<List<Message>> publishCreditReservationEvents(Long customerId, DomainEvent customerEvent, String orderId, DomainEvent orderSagaEvent) {
    return domainEventPublisher
                    .aggregateType(Customer.class)
                        .aggregateId(customerId)
                        .event(customerEvent)
                    .aggregateType("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order")
                      .aggregateId(orderId)
                      .event(orderSagaEvent)
                    .publish();
  }

  private Mono<List<Message>> handleNotExistingCustomer(String orderId, long customerId, Money orderTotal) {
    return Mono.defer(() -> {
      logger.info("non existent customer {}", customerId);
      CreateOrderSagaStepFailedEvent stepFailedEvent =
              new CreateOrderSagaStepFailedEvent("Customer Service", new OrderDetails(customerId, orderTotal));
      return publishCreditReservationEvents(customerId, new CustomerValidationFailedEvent(orderId), orderId, stepFailedEvent);
    });
  }
}
