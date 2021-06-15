package io.eventuate.examples.tram.ordersandcustomers.common;

import java.util.UUID;

public class IdGenerator {
  public static String generateId() {
    return UUID.randomUUID().toString();
  }
}
