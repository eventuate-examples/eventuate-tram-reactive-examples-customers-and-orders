package io.eventuate.examples.tram.ordersandcustomers.customers.web;

import io.eventuate.examples.tram.ordersandcustomers.customers.service.CustomerService;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCreatedEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class OrderEventHttpConsumer {

  private CustomerService customerService;

  @Autowired
  public OrderEventHttpConsumer(CustomerService customerService) {
    this.customerService = customerService;
  }

  @RequestMapping(value = "/events/orderserviceevents/io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order/{aggregateId}/io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCreatedEvent/{eventId}", method = RequestMethod.POST)
  public Mono<Void> handleOrderCreatedEvent(@RequestBody OrderCreatedEvent event, @PathVariable("aggregateId") String aggregateId) {
    return customerService.reserveCredit(Long.parseLong(aggregateId),
            event.getOrderDetails().getCustomerId(), event.getOrderDetails().getOrderTotal());
  }

  @RequestMapping(value = "/events/orderserviceevents/io.eventuate.examples.tram.ordersandcustomers.orders.domain.Order/{aggregateId}/io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderCanceledEvent/{eventId}", method = RequestMethod.POST)
  public Mono<Void> handleOrderCanceledEvent(@RequestBody OrderCreatedEvent event, @PathVariable("aggregateId") String aggregateId) {
    return customerService.releaseCredit(Long.parseLong(aggregateId), event.getOrderDetails().getCustomerId());
  }
}
