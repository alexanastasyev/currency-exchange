package order;

import model.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class OrderAsyncTest {

    @RepeatedTest(10)
    public void createManyOrdersFromOneClientTest() {

        Client client = new Client(1);
        client.deposit(Currency.EUR, new BigDecimal(100_000));

        CountDownLatch countDownLatch = new CountDownLatch(1);

        Runnable createOrderTask = () -> {
            try {
                countDownLatch.await();
                Order order = new Order(client, CurrencyPair.EUR_GBP, OrderType.SELL, new BigDecimal(1000), new BigDecimal(2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            threads.add(new Thread(createOrderTask));
        }
        threads.forEach(Thread::start);
        countDownLatch.countDown();

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Assert.assertEquals(0, client.getBalance().get(Currency.EUR).compareTo(new BigDecimal(50_000)));

    }

}
