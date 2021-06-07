package io.eventuate.examples.tram.ordersandcustomers.endtoendtests;

import io.eventuate.common.json.mapper.JSonMapper;
import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import io.eventuate.examples.tram.ordersandcustomers.customers.webapi.CreateCustomerRequest;
import io.eventuate.examples.tram.ordersandcustomers.customers.webapi.CreateCustomerResponse;
import io.eventuate.examples.tram.ordersandcustomers.orders.domain.events.OrderState;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderRequest;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.CreateOrderResponse;
import io.eventuate.examples.tram.ordersandcustomers.orders.webapi.GetOrderResponse;
import io.eventuate.util.test.async.Eventually;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CustomersAndOrdersEndToEndTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CustomersAndOrdersEndToEndTest {

  @Value("${host.name}")
  private String hostName;

  private String baseUrlOrdersProxy(String... path) {
    return baseUrlOrders(8083, path);
  }

  private String baseUrlOrders(String... path) {
    return baseUrlOrders(8081, path);
  }

  private String baseUrlOrders(int port, String... path) {
    StringBuilder sb = new StringBuilder();
    sb.append("http://").append(hostName).append(":" + port);
    Arrays.stream(path).forEach(p -> {
      sb.append('/').append(p);
    });
    return sb.toString();
  }

  private String baseUrlCustomers(String path) {
    return "http://"+hostName+":8082/" + path;
  }

  @Autowired
  RestTemplate restTemplate;

  @Test
  public void shouldApprove() {
    Long customerId = createCustomer("Fred", new Money("15.00"));
    String orderId = createOrder(customerId, new Money("12.34"), HttpStatus.OK);
    assertOrderState(orderId, OrderState.APPROVED);
  }

  @Test
  public void shouldReject() {
    Long customerId = createCustomer("Fred", new Money("15.00"));
    String orderId = createOrder(customerId, new Money("123.34"), HttpStatus.BAD_REQUEST);
    assertOrderState(orderId, OrderState.REJECTED);
  }

  @Test
  public void shouldRejectForNonExistentCustomerId() {
    Long customerId = System.nanoTime();
    String orderId = createOrder(customerId, new Money("123.34"), HttpStatus.BAD_REQUEST);
    assertOrderState(orderId, OrderState.REJECTED);
  }

  @Test
  public void shouldCancel() {
    Long customerId = createCustomer("Fred", new Money("15.00"));
    String orderId = createOrder(customerId, new Money("12.34"), HttpStatus.OK);
    assertOrderState(orderId, OrderState.APPROVED);
    cancelOrder(orderId);
    assertOrderState(orderId, OrderState.CANCELLED);
  }

  private Long createCustomer(String name, Money credit) {
    return restTemplate.postForObject(baseUrlCustomers("customers"),
            new CreateCustomerRequest(name, credit), CreateCustomerResponse.class).getCustomerId();
  }

  private String createOrder(Long customerId, Money orderTotal, HttpStatus expectedStatus) {
    try {
      ResponseEntity<CreateOrderResponse> createOrderResponse = restTemplate.postForEntity(baseUrlOrdersProxy("orders"),
              new CreateOrderRequest(customerId, orderTotal), CreateOrderResponse.class);

      Assert.assertEquals(expectedStatus, createOrderResponse.getStatusCode());

      return createOrderResponse.getBody().getOrderId();
    } catch (HttpStatusCodeException e) {
      Assert.assertEquals(expectedStatus, e.getStatusCode());
      return JSonMapper.fromJson(e.getResponseBodyAsString(), CreateOrderResponse.class).getOrderId();
    }
  }

  private void cancelOrder(String orderId) {
    restTemplate.postForObject(baseUrlOrders("orders", orderId, "cancel"),
            null, GetOrderResponse.class);
  }

  private void assertOrderState(String id, OrderState expectedState) {
    Eventually.eventually(120, 250, TimeUnit.MILLISECONDS, () -> {
      ResponseEntity<GetOrderResponse> response =
              restTemplate.getForEntity(baseUrlOrders("orders/" + id), GetOrderResponse.class);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      GetOrderResponse order = response.getBody();

      assertEquals(expectedState, order.getOrderState());
    });
  }
}
