package stockmarket;

import model.*;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;


public class StockMarketEfficiencyTest {
    @Test
    public void createManyRandomOrdersTest() {
        StockMarket stockMarket = new StockMarket();
        CountDownLatch countDownLatch = new CountDownLatch(1);

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            threads.add(new Thread(() -> {
                try {
                    countDownLatch.await();
                    for (int j = 0; j < 10_000; j++) {

                        Client client = new Client(j);
                        client.deposit(Currency.USD, new BigDecimal(new Random().nextInt(100_001) + 10_000));
                        client.deposit(Currency.EUR, new BigDecimal(new Random().nextInt(100_001) + 10_000));
                        client.deposit(Currency.RUB, new BigDecimal(new Random().nextInt(100_001) + 10_000));

                        CurrencyPair currencyPair;
                        switch (new Random().nextInt(3)) {
                            case 0: {
                                currencyPair = CurrencyPair.USD_EUR;
                                break;
                            }
                            case 1: {
                                currencyPair = CurrencyPair.USD_RUB;
                                break;
                            }
                            case 2: {
                                currencyPair = CurrencyPair.EUR_RUB;
                                break;
                            }
                            default: {
                                throw new RuntimeException();
                            }
                        }

                        OrderType orderType;
                        switch (new Random().nextInt(2)) {
                            case 0: {
                                orderType = OrderType.BUY;
                                break;
                            }
                            case 1: {
                                orderType = OrderType.SELL;
                                break;
                            }
                            default: {
                                throw new RuntimeException();
                            }
                        }

                        BigDecimal amount = new BigDecimal(new Random().nextInt(11) + 1);
                        BigDecimal price = new BigDecimal(new Random().nextInt(101) + 10);

                        Order order = new Order(client, currencyPair, orderType, amount, price);

                        stockMarket.addOrder(order);
                    }
                } catch (InterruptedException e) {
                    System.out.println(e.getMessage());
                }
            }));
        }

        long startTime = System.currentTimeMillis();

        threads.forEach(Thread::start);
        countDownLatch.countDown();
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException();
            }
        });

        long endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");
        System.out.println("Speed: " + 100_000.0 / ((endTime - startTime) / 1000.0) + " orders / second");
    }
}
