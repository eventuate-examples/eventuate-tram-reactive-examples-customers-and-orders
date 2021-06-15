package io.eventuate.examples.tram.ordersandcustomers.apigateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "gateway.destinations")
public class DestinationConfigurationProperties {

  @NotNull
  private String orderServiceUrl;

  @NotNull
  private String customerServiceUrl;

  public String getOrderServiceUrl() {
    return orderServiceUrl;
  }

  public void setOrderServiceUrl(String orderServiceUrl) {
    this.orderServiceUrl = orderServiceUrl;
  }

  public String getCustomerServiceUrl() {
    return customerServiceUrl;
  }

  public void setCustomerServiceUrl(String customerServiceUrl) {
    this.customerServiceUrl = customerServiceUrl;
  }
}
