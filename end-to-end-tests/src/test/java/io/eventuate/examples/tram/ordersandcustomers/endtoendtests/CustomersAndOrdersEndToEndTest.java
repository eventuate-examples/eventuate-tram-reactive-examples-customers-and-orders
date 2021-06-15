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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CustomersAndOrdersEndToEndTestConfiguration.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class CustomersAndOrdersEndToEndTest {

  @Value("${host.name}")
  private String hostName;

  private String baseUrl(String... path) {
    StringBuilder sb = new StringBuilder();
    sb.append("http://").append(hostName).append(":8083");
    Arrays.stream(path).forEach(p -> {
      sb.append('/').append(p);
    });
    return sb.toString();
  }


  @Autowired
  RestTemplate restTemplate;

  @Test
  public void shouldSupportTimeout() throws IOException, InterruptedException {
    try {
      executeScript("stop-order-service.sh");
      Long customerId = createCustomer("Fred", new Money("15.00"));
      createOrder(customerId, new Money("12.34"), HttpStatus.ACCEPTED);
    } finally {
      executeScript("start-order-service.sh");
    }
  }

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

  private void executeScript(String script) throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder();
    processBuilder.directory(new File(".."));
    processBuilder.command("sh", script);
    processBuilder.inheritIO();
    processBuilder.start().waitFor();
  }

  private Long createCustomer(String name, Money credit) {
    return restTemplate.postForObject(baseUrl("customers"),
            new CreateCustomerRequest(name, credit), CreateCustomerResponse.class).getCustomerId();
  }

  private String createOrder(Long customerId, Money orderTotal, HttpStatus expectedStatus) {
    try {
      ResponseEntity<CreateOrderResponse> createOrderResponse = restTemplate.postForEntity(baseUrl("orders"),
              new CreateOrderRequest(customerId, orderTotal), CreateOrderResponse.class);

      Assert.assertEquals(expectedStatus, createOrderResponse.getStatusCode());

      return createOrderResponse.getBody().getOrderId();
    } catch (HttpStatusCodeException e) {
      Assert.assertEquals(expectedStatus, e.getStatusCode());
      return JSonMapper.fromJson(e.getResponseBodyAsString(), CreateOrderResponse.class).getOrderId();
    }
  }

  private void cancelOrder(String orderId) {
    restTemplate.postForObject(baseUrl("orders", orderId, "cancel"),
            null, GetOrderResponse.class);
  }

  private void assertOrderState(String id, OrderState expectedState) {
    Eventually.eventually(120, 250, TimeUnit.MILLISECONDS, () -> {
      ResponseEntity<GetOrderResponse> response =
              restTemplate.getForEntity(baseUrl("orders/" + id), GetOrderResponse.class);

      assertEquals(HttpStatus.OK, response.getStatusCode());

      GetOrderResponse order = response.getBody();

      assertEquals(expectedState, order.getOrderState());
    });
  }
}
