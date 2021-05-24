package io.eventuate.examples.tram.ordersandcustomers.orders.web;

import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderDetails;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.OrderRepository;
import io.eventuate.examples.tram.ordersandcustomers.orders.service.OrderService;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.GetOrderResponse;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
public class OrderController {

  private OrderService orderService;
  private OrderRepository orderRepository;

  @Autowired
  public OrderController(OrderService orderService,
                         OrderRepository orderRepository) {

    this.orderService = orderService;
    this.orderRepository = orderRepository;
  }

  @RequestMapping(value = "/orders", method = RequestMethod.POST)
  public Mono<CreateOrderResponse> createOrder(@RequestBody CreateOrderRequest createOrderRequest) {
    return orderService
            .createOrder(new OrderDetails(createOrderRequest.getCustomerId(), createOrderRequest.getOrderTotal()))
            .map(o -> new CreateOrderResponse(o.getId()));
  }

  @RequestMapping(value="/orders/{orderId}", method= RequestMethod.GET)
  public Mono<ResponseEntity<GetOrderResponse>> getOrder(@PathVariable Long orderId) {
     return orderRepository
            .findById(orderId)
            .map(this::makeSuccessfulResponse)
            .switchIfEmpty(Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
  }

  @RequestMapping(value="/orders/{orderId}/cancel", method= RequestMethod.POST)
  public Mono<ResponseEntity<GetOrderResponse>> cancelOrder(@PathVariable Long orderId) {
     return orderService
             .cancelOrder(orderId)
             .map(this::makeSuccessfulResponse)
             .switchIfEmpty(Mono.just(new ResponseEntity<>(HttpStatus.NOT_FOUND)));
  }

  private ResponseEntity<GetOrderResponse> makeSuccessfulResponse(Order order) {
    return new ResponseEntity<>(new GetOrderResponse(order.getId(), order.getState()), HttpStatus.OK);
  }
}