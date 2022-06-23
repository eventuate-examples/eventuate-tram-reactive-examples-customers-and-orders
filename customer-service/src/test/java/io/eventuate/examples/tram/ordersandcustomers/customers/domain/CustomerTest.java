package io.eventuate.examples.tram.ordersandcustomers.customers.domain;

import io.eventuate.examples.tram.ordersandcustomers.common.domain.Money;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CustomerTest  {

    private Customer customer;
    private String orderId = "101";

    @Before
    public void setUp() {
        this.customer = new Customer("Fred", new BigDecimal(10));
    }

    @Test
    public void shouldReserve() {
        assertTrue(reserve(5).isPresent());
    }

    private Optional<CreditReservation> reserve(int orderTotal, CreditReservation... creditReservations) {
        return customer.attemptToReserveCredit(Arrays.asList(creditReservations), orderId, new Money(orderTotal));
    }

    @Test
    public void shouldReserveWithExisting() {
        assertTrue(reserve(5, new CreditReservation(null, null, new BigDecimal(3))).isPresent());
    }

    @Test
    public void shouldReserveAll() {
        assertTrue(reserve(10).isPresent());
    }

    @Test
    public void shouldFailToReserve() {
        assertFalse(reserve(20).isPresent());
    }

}