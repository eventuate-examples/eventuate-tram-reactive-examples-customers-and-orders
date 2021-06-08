package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.common.IdGenerator;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventDispatcher;
import io.eventuate.tram.reactive.events.subscriber.ReactiveDomainEventDispatcherFactory;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import io.eventuate.tram.spring.events.publisher.ReactiveTramEventsPublisherConfiguration;
import io.eventuate.tram.spring.messaging.producer.jdbc.reactive.ReactiveTramMessageProducerJdbcConfiguration;
import io.eventuate.tram.spring.reactive.consumer.common.ReactiveTramConsumerCommonConfiguration;
import io.eventuate.tram.spring.reactive.consumer.kafka.EventuateTramReactiveKafkaMessageConsumerConfiguration;
import io.eventuate.tram.spring.reactive.events.subscriber.ReactiveTramEventSubscriberConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

@Configuration
@Import({ReactiveTramMessageProducerJdbcConfiguration.class,
        ReactiveTramEventsPublisherConfiguration.class,
        ReactiveTramEventSubscriberConfiguration.class,
        ReactiveTramConsumerCommonConfiguration.class,
        EventuateTramReactiveKafkaMessageConsumerConfiguration.class
})
@ComponentScan
@EnableAutoConfiguration
@EnableConfigurationProperties(DestinationConfigurationProperties.class)
public class GatewayConfiguration {

  @Bean
  public RouteLocator orderProxyRouting(RouteLocatorBuilder builder, DestinationConfigurationProperties destinationConfigurationProperties) {
    return builder.routes()
            .route(r -> r.path("/orders/**").and().method("GET").uri(destinationConfigurationProperties.getOrderServiceUrl()))
            .route(r -> r.path("/orders/**").and().method("POST").uri(destinationConfigurationProperties.getOrderServiceUrl()))
            .route(r -> r.path("/customers").and().method("POST").uri(destinationConfigurationProperties.getCustomerServiceUrl()))
            .build();
  }

  @Bean
  public OrderHandlers orderHandlers(ReactiveDomainEventPublisher domainEventPublisher,
                                     TransactionalOperator transactionalOperator,
                                     OrderEventConsumer orderEventConsumer) {
    return new OrderHandlers(domainEventPublisher, transactionalOperator, orderEventConsumer);
  }

  @Bean
  public RouterFunction<ServerResponse> orderHandlerRouting(OrderHandlers orderHandlers) {
    return RouterFunctions.route(POST("/orders"), orderHandlers::createOrder);
  }

  @Bean
  public OrderEventConsumer orderEventConsumer() {
    return new OrderEventConsumer();
  }

  @Bean
  public ReactiveDomainEventDispatcher domainEventDispatcher(OrderEventConsumer orderEventConsumer,
                                                             ReactiveDomainEventDispatcherFactory domainEventDispatcherFactory) {
    return domainEventDispatcherFactory.make(IdGenerator.generateId(), orderEventConsumer.domainEventHandlers());
  }
}
