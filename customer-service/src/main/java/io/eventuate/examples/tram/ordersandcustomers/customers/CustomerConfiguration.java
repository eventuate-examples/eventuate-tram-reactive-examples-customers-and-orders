package io.eventuate.examples.tram.ordersandcustomers.customers;

import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CustomerRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.domain.CreditReservationRepository;
import io.eventuate.examples.tram.ordersandcustomers.customers.service.CustomerService;
import io.eventuate.examples.tram.ordersandcustomers.customers.service.OrderEventConsumer;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventDispatcher;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventDispatcherFactory;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.ReactiveTramEventsPublisherConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.reactive.ReactiveTramMessageProducerJdbcConfiguration;
import io.eventuate.tram.spring.reactive.consumer.common.ReactiveTramConsumerCommonConfiguration;
import io.eventuate.tram.spring.reactive.consumer.kafka.EventuateTramReactiveKafkaMessageConsumerConfiguration;
import io.eventuate.tram.spring.reactive.events.subscriber.ReactiveTramEventSubscriberConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.transaction.reactive.TransactionalOperator;

@Configuration
@Import({ReactiveTramMessageProducerJdbcConfiguration.class,
        ReactiveTramEventsPublisherConfiguration.class,
        ReactiveTramEventSubscriberConfiguration.class,
        ReactiveTramConsumerCommonConfiguration.class,
        EventuateTramReactiveKafkaMessageConsumerConfiguration.class

})
@EnableAutoConfiguration
@EnableR2dbcRepositories
public class CustomerConfiguration {

  @Bean
  public OrderEventConsumer orderEventConsumer(CustomerService customerService) {
    return new OrderEventConsumer(customerService);
  }

  @Bean
  public ReactiveDomainEventDispatcher domainEventDispatcher(OrderEventConsumer orderEventConsumer,
                                                             ReactiveDomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("orderServiceEvents", orderEventConsumer.domainEventHandlers());
  }

  @Bean
  public CustomerService customerService(CustomerRepository customerRepository,
                                         CreditReservationRepository creditReservationRepository,
                                         ReactiveDomainEventPublisher domainEventPublisher,
                                         TransactionalOperator transactionalOperator) {

    return new CustomerService(customerRepository, creditReservationRepository, domainEventPublisher, transactionalOperator);
  }
}
