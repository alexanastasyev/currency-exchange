package stockmarket;

import model.*;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import util.CurrencyUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class StockMarketAsyncTest {

    private StockMarket stockMarket;

    @BeforeEach
    public void beforeEach() {
        stockMarket = new StockMarketSimple();
    }

    @RepeatedTest(50)
    public void createTwoMatchingOrdersTest() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Client client1 = new Client(1);
        client1.deposit(Currency.RUB, new BigDecimal(1000));
        Order order1 = new Order(client1, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(15), new BigDecimal(66.66));

        Runnable postOrder1 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread1 = new Thread(postOrder1);

        Client client2 = new Client(2);
        client2.deposit(Currency.USD,  new BigDecimal(15));
        Order order2 = new Order(client2, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));

        Runnable postOrder2 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread2 = new Thread(postOrder2);

        thread1.start();
        thread2.start();

        countDownLatch.countDown();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(0, stockMarket.getAllOrdersList().size());
        Assert.assertEquals(0, client1.getBalance().get(Currency.RUB).add(client2.getBalance().get(Currency.RUB)).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE).compareTo(new BigDecimal(1000)));
        Assert.assertEquals(0, client1.getBalance().get(Currency.USD).add(client2.getBalance().get(Currency.USD)).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE).compareTo(new BigDecimal(15)));

    }

    @RepeatedTest(50)
    public void createThreeMatchingOrdersTest() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Client client1 = new Client(1);
        client1.deposit(Currency.USD, new BigDecimal(15));
        Order order1 = new Order(client1, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        Runnable postOrder1 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread1 = new Thread(postOrder1);

        Client client2 = new Client(2);
        client2.deposit(Currency.USD, new BigDecimal(25));
        Order order2 = new Order(client2, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(25), new BigDecimal(70));
        Runnable postOrder2 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread2 = new Thread(postOrder2);

        Client client3 = new Client(3);
        client3.deposit(Currency.RUB, new BigDecimal(10_000));
        Order order3 = new Order(client3, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(40), new BigDecimal(80));
        Runnable postOrder3 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order3);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread3 = new Thread(postOrder3);

        thread1.start();
        thread2.start();
        thread3.start();

        countDownLatch.countDown();

        try {
            thread1.join();
            thread2.join();
            thread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Assert.assertEquals(0, stockMarket.getAllOrdersList().size());
        Assert.assertEquals(0, client3.getBalance().get(Currency.USD).compareTo(new BigDecimal(40)));

        BigDecimal rublesSum = client1.getBalance().get(Currency.RUB)
                .add(client2.getBalance().get(Currency.RUB))
                .add(client3.getBalance().get(Currency.RUB))
                .setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);

        Assert.assertEquals(0, rublesSum.compareTo(new BigDecimal(10_000)));
    }

    @RepeatedTest(50)
    public void createTwoNonMatchingOrdersThenRevokeAllTest() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Client client1 = new Client(1);
        client1.deposit(Currency.USD, new BigDecimal(15));
        Order order1 = new Order(client1, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(15), new BigDecimal(65));
        Runnable postOrder1 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread1 = new Thread(postOrder1);

        Client client2 = new Client(2);
        client2.deposit(Currency.USD, new BigDecimal(25));
        Order order2 = new Order(client2, CurrencyPair.USD_RUB, OrderType.SELL, new BigDecimal(25), new BigDecimal(70));
        Runnable postOrder2 = () -> {
            try {
                countDownLatch.await();
                stockMarket.addOrder(order2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        Thread thread2 = new Thread(postOrder2);

        thread1.start();
        thread2.start();

        countDownLatch.countDown();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stockMarket.revokeAllOrders();

        Assert.assertEquals(0, stockMarket.getAllOrdersList().size());
        Assert.assertEquals(0, client1.getBalance().get(Currency.USD).compareTo(new BigDecimal(15)));
        Assert.assertEquals(0, client2.getBalance().get(Currency.USD).compareTo(new BigDecimal(25)));

    }

    @RepeatedTest(50)
    public void createManyRandomOrdersAndCheckSumsTest() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        List<Client> clients = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Client client = new Client(i + 1);
            client.deposit(Currency.USD, new BigDecimal(new Random().nextInt(100_001) + 10_000));
            client.deposit(Currency.EUR, new BigDecimal(new Random().nextInt(100_001) + 10_000));
            client.deposit(Currency.RUB, new BigDecimal(new Random().nextInt(100_001) + 10_000));
            clients.add(client);
        }

        BigDecimal rubSumBefore = BigDecimal.ZERO;
        BigDecimal usdSumBefore = BigDecimal.ZERO;
        BigDecimal eurSumBefore = BigDecimal.ZERO;

        for (Client client : clients) {
            rubSumBefore = rubSumBefore.add(client.getBalance().get(Currency.RUB));
            usdSumBefore = usdSumBefore.add(client.getBalance().get(Currency.USD));
            eurSumBefore = eurSumBefore.add(client.getBalance().get(Currency.EUR));
        }

        List<Order> orders = new ArrayList<>();
        clients.forEach(client -> {
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
            orders.add(order);
        });

        List<Thread> threads = new ArrayList<>();
        orders.forEach(order -> {
            threads.add(new Thread(() -> {
                try {
                    countDownLatch.await();
                    stockMarket.addOrder(order);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }));
        });

        threads.forEach(Thread::start);
        countDownLatch.countDown();

        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("After all deals orders left: " + stockMarket.getAllOrdersList().size() + " of 100");

        stockMarket.revokeAllOrders();

        BigDecimal rubSumAfter = BigDecimal.ZERO;
        BigDecimal usdSumAfter = BigDecimal.ZERO;
        BigDecimal eurSumAfter = BigDecimal.ZERO;

        for (Client client : clients) {
            rubSumAfter = rubSumAfter.add(client.getBalance().get(Currency.RUB));
            usdSumAfter = usdSumAfter.add(client.getBalance().get(Currency.USD));
            eurSumAfter = eurSumAfter.add(client.getBalance().get(Currency.EUR));
        }

        Assert.assertEquals(0, rubSumBefore.compareTo(rubSumAfter));
        Assert.assertEquals(0, eurSumBefore.compareTo(eurSumAfter));
        Assert.assertEquals(0, usdSumBefore.compareTo(usdSumAfter));

    }

}
