package io.eventuate.examples.tram.ordersandcustomers.orders;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.OrderRepository;
import io.eventuate.examples.tram.ordersandcustomers.orders.service.CustomerEventConsumer;
import io.eventuate.examples.tram.ordersandcustomers.orders.service.OrderService;
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
public class OrderConfiguration {

  @Bean
  public OrderService orderService(ReactiveDomainEventPublisher domainEventPublisher,
                                   OrderRepository orderRepository,
                                   TransactionalOperator transactionalOperator) {
    return new OrderService(domainEventPublisher, orderRepository, transactionalOperator);
  }


  @Bean
  public CustomerEventConsumer orderEventConsumer() {
    return new CustomerEventConsumer();
  }

  @Bean
  public ReactiveDomainEventDispatcher domainEventDispatcher(CustomerEventConsumer customerEventConsumer,
                                                             ReactiveDomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make("customerServiceEvents", customerEventConsumer.domainEventHandlers());
  }
}
