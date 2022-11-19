package client;

import model.Client;
import model.Currency;
import org.junit.Assert;
import org.junit.jupiter.api.RepeatedTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ClientAsyncTest {

    @RepeatedTest(50)
    public void depositFromManyThreadsTest() {
        Client client = new Client(1);
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Runnable depositMoneyTask = () -> {
            try {
                countDownLatch.await();
                client.deposit(Currency.RUB, new BigDecimal(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            threads.add(new Thread(depositMoneyTask));
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

        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(50_000)));
    }

    @RepeatedTest(50)
    public void depositAndWithDrawFromManyThreadsTest() {
        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000));
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Runnable depositMoneyTask = () -> {
            try {
                countDownLatch.await();
                client.deposit(Currency.RUB, new BigDecimal(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Runnable withdrawMoneyTask = () -> {
            try {
                countDownLatch.await();
                client.withdraw(Currency.RUB, new BigDecimal(2));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            threads.add(new Thread(depositMoneyTask));
            threads.add(new Thread(withdrawMoneyTask));
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

        Assert.assertEquals(0, client.getBalance().get(Currency.RUB).compareTo(new BigDecimal(50_900)));
    }

}
