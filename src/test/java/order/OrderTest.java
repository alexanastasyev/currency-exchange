package order;

import exception.NotEnoughMoneyException;
import model.*;
import org.junit.Assert;
import org.junit.Test;
import util.CurrencyUtils;

import java.math.BigDecimal;

public class OrderTest {

    @Test
    public void createPossibleOrderBuyTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(60));
        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(400)));
    }

    @Test
    public void createPossibleOrderSellTest() {
        Client client = new Client(1);
        client.deposit(Currency.USD, new BigDecimal(300));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(300), new BigDecimal(65));
        Assert.assertEquals(0, client.getBalance().get(Currency.USD).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void createImpossibleOrderBuyTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Assert.assertThrows(NotEnoughMoneyException.class, () -> {
            Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(200), new BigDecimal(63.50));
        });
    }

    @Test
    public void createImpossibleOrderSellTest() {
        Client client = new Client(1);
        client.deposit(Currency.USD, new BigDecimal(399.99));
        Assert.assertThrows(NotEnoughMoneyException.class, () -> {
            Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(400), new BigDecimal(61.50));
        });
    }

    @Test
    public void reduceOrderPossibleTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(60));
        order.reduce(new BigDecimal(4.78), order.getPrice());
        Assert.assertEquals(0, order.getAmount().compareTo(new BigDecimal(5.22).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE)));
        Assert.assertEquals(0, client.getBalance().get(Currency.USD).compareTo(new BigDecimal(4.78).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE)));
    }

    @Test
    public void reduceOrderImpossibleTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(60));
        Assert.assertThrows(NotEnoughMoneyException.class, () -> {
           order.reduce(new BigDecimal(10.01), order.getPrice());
        });
    }

    @Test
    public void revokeOrderBuyTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(60));
        order.revoke();
        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(1000)));
    }

    @Test
    public void revokeOrderSellTest() {
        Client client = new Client(1);
        client.deposit(Currency.USD, new BigDecimal(300));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(300), new BigDecimal(65));
        order.revoke();
        Assert.assertEquals(0, client.getBalance().get(Currency.USD).compareTo(new BigDecimal(300)));
    }

    @Test
    public void reduceOrderThenRevokeTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(60));
        order.reduce(new BigDecimal(5), order.getPrice());
        Assert.assertEquals(0, client.getBalance().get(Currency.USD).compareTo(new BigDecimal(5)));
        order.revoke();
        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(700)));
    }

}
