package io.eventuate.examples.tram.ordersandcustomers.customers;

import io.eventuate.examples.tram.ordersandcustomers.customers.web.CustomerWebConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication
@Import({CustomerConfiguration.class, CustomerWebConfiguration.class})
public class CustomerServiceMain {
  public static void main(String[] args) {
    ReactorDebugAgent.init();
    SpringApplication.run(CustomerServiceMain.class, args);
  }
}
