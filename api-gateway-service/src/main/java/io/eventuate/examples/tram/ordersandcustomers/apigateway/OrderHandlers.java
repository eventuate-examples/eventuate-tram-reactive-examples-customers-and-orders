package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import io.eventuate.examples.tram.ordersandcustomers.common.IdGenerator;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.CreateOrderSagaStartedEvent;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.events.publisher.ReactiveDomainEventPublisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public class OrderHandlers {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private ReactiveDomainEventPublisher domainEventPublisher;
  private TransactionalOperator transactionalOperator;
  private OrderEventConsumer orderEventConsumer;

  public OrderHandlers(ReactiveDomainEventPublisher domainEventPublisher,
                       TransactionalOperator transactionalOperator,
                       OrderEventConsumer orderEventConsumer) {
    this.domainEventPublisher = domainEventPublisher;
    this.transactionalOperator = transactionalOperator;
    this.orderEventConsumer = orderEventConsumer;
  }


  public Mono<ServerResponse> createOrder(ServerRequest serverRequest) {
    return serverRequest
            .bodyToMono(CreateOrderRequest.class)
            .flatMap(createOrderRequest -> {
              String orderAndSagaId = IdGenerator.generateId();

              CreateOrderSagaStartedEvent createOrderSagaStartedEvent =
                      new CreateOrderSagaStartedEvent(new OrderDetails(createOrderRequest.getCustomerId(), createOrderRequest.getOrderTotal()));

              Mono<OrderState> response = orderEventConsumer.prepareCreateOrderResponse(orderAndSagaId);

              logger.info("Initiating createOrderSaga {}", orderAndSagaId);

                return domainEventPublisher
                        .publish("io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order",
                                orderAndSagaId, createOrderSagaStartedEvent)
                        .as(transactionalOperator::transactional)
                      .then(response)
                      .flatMap(orderState -> createServerResponse(orderAndSagaId, orderState));
            });
  }


  private Mono<ServerResponse> createServerResponse(String orderId, OrderState orderState) {
    CreateOrderResponse createOrderResponse = new CreateOrderResponse(orderId);

    HttpStatus status;

    switch (orderState) {
      case FAILED: {
        status = HttpStatus.BAD_REQUEST;
        break;
      }
      case COMPLETE: {
        status = HttpStatus.OK;
        break;
      }
      case TIMEOUT: {
        status = HttpStatus.ACCEPTED;
        break;
      }
      default: {
        throw new IllegalStateException(); //Should not be here
      }
    }

    logger.info("createOrderSaga {} HttpStatus {}", orderId, status);

    return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).bodyValue(createOrderResponse);
  }

    private class TestMessageSubscriber implements Subscriber<Message> {

        private final String label;

        public TestMessageSubscriber(String label) {
            this.label = label;
        }

        @Override
        public void onSubscribe(Subscription s) {
            logger.info("{} onSubscribe", label);
            s.request(1);
        }

        @Override
        public void onNext(Message message) {
            logger.info("{} onNext {}", label, message);
        }

        @Override
        public void onError(Throwable t) {
            logger.info("{} onError ", label);
        }

        @Override
        public void onComplete() {
            logger.info("{} onComplete", label);
        }
    }
}
