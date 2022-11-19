package client;

import exception.NotEnoughMoneyException;
import model.Client;
import model.Currency;
import org.junit.Assert;
import org.junit.Test;
import util.CurrencyUtils;

import java.math.BigDecimal;

public class ClientTest {

    @Test
    public void createNewClientTest() {
        Client client = new Client(1);
        for(BigDecimal value : client.getBalance().values()) {
            Assert.assertEquals(0, value.compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    public void deposit1000RUBTest() {
        BigDecimal money = new BigDecimal(1000);
        Client client = new Client(1);
        client.deposit(Currency.RUB, money);
        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(money));
    }

    @Test
    public void deposit1000RUBThenWithdrawSomeTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        client.withdraw(Currency.RUB, new BigDecimal(333.33));
        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(666.67).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE)));
    }

    @Test
    public void deposit1000USD55CentThenWithdrawAllTest() {
        Client client = new Client(1);
        client.deposit(Currency.USD, new BigDecimal(1000.55));
        client.withdraw(Currency.USD, new BigDecimal(1000.55));
        Assert.assertEquals(0, client.getBalance().get(Currency.USD).compareTo(BigDecimal.ZERO));
    }

    @Test
    public void deposit400EURThenWithdraw401Test() {
        Client client = new Client(1);
        client.deposit(Currency.EUR, new BigDecimal(400));
        Assert.assertThrows(NotEnoughMoneyException.class, () -> {
            client.withdraw(Currency.EUR, new BigDecimal(401));
        });
    }

    @Test
    public void createClientThenWithdraw1CentTest() {
        Client client = new Client(1);
        Assert.assertThrows(NotEnoughMoneyException.class, () -> {
            client.withdraw(Currency.USD, new BigDecimal(0.01));
        });
    }

}
