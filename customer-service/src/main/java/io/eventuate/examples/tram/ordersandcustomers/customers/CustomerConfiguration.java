package io.eventuate.examples.tram.ordersandcustomers.customers;

import io.eventuate.common.spring.jdbc.reactive.EventuateCommonReactiveMysqlConfiguration;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CustomerRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CreditReservationRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.service.CustomerService;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.ReactiveTramEventsPublisherConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.reactive.ReactiveTramMessageProducerJdbcConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@Import({ReactiveTramMessageProducerJdbcConfiguration.class,
        EventuateCommonReactiveMysqlConfiguration.class,
        ReactiveTramEventsPublisherConfiguration.class})
@EnableAutoConfiguration
@EnableR2dbcRepositories
public class CustomerConfiguration {

  @Bean
  public CustomerService customerService(CustomerRepository customerRepository,
                                         CreditReservationRepository creditReservationRepository,
                                         ReactiveDomainEventPublisher domainEventPublisher,
                                         TransactionalOperator transactionalOperator) {

    return new CustomerService(customerRepository, creditReservationRepository, domainEventPublisher, transactionalOperator);
  }
}
