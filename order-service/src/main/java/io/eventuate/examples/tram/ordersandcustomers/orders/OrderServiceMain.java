package io.eventuate.examples.tram.ordersandcustomers.orders;

import io.eventuate.examples.tram.ordersandcustomers.orders.web.OrderWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
@Import({OrderWebConfiguration.class, OrderConfiguration.class})
public class OrderServiceMain {
  public static void main(String[] args) {
    ReactorDebugAgent.init();
    SpringApplication.run(OrderServiceMain.class, args);
  }
}
