package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import reactor.tools.agent.ReactorDebugAgent;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@Import(GatewayConfiguration.class)
public class ApiGatewayMain {

  public static void main(String[] args) {
    ReactorDebugAgent.init();
    SpringApplication.run(ApiGatewayMain.class, args);
  }
}

