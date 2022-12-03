package stockmarket;

import exception.UnsupportedOrderTypeException;
import model.CurrencyPair;
import model.Order;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class StockMarket {

    private final Map<CurrencyPair, List<Order>> orders;

    private final BlockingQueue<Order> ordersQueue;

    private final Thread threadConsumer;

    public StockMarket() {
        this.orders = new ConcurrentHashMap<>(CurrencyPair.values().length);
        for (CurrencyPair currencyPair : CurrencyPair.values()) {
            orders.put(currencyPair, new ArrayList<>());
        }

        this.ordersQueue = new LinkedBlockingQueue<>();
        threadConsumer = new Thread(() -> {
            while (true) {
                Order order = ordersQueue.poll();
                if (order == null) {
                    continue;
                }
                consumeOrder(order);
            }
        });
        threadConsumer.setDaemon(true);
        threadConsumer.start();
    }

    public void addOrder(Order order) {
        ordersQueue.add(order);
    }

    public void consumeOrder(Order order) {
        List<Order> orderCandidates = orders.get(order.getCurrencyPair()).stream()
                .filter(orderCandidate -> filterByClient(order, orderCandidate))
                .filter(orderCandidate -> filterByType(order, orderCandidate))
                .filter(orderCandidate -> filterByPrice(order, orderCandidate))
                .sorted((order1, order2) -> compareOrdersForSorting(order1, order2, order))
                .collect(Collectors.toList());

        orderCandidates.forEach(orderCandidate -> {
            BigDecimal dealAmount = orderCandidate.getAmount().min(order.getAmount());
            BigDecimal dealPrice = getDealPrice(order, orderCandidate);

            if (reduceOrder(orderCandidate, dealAmount, dealPrice)) {
                closeOrder(orderCandidate);
            }
            if (reduceOrder(order, dealAmount, dealPrice)) {
                order.revoke();
            }
        });

        if (order.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            orders.get(order.getCurrencyPair()).add(order);
        }
    }

    public List<Order> getAllOrdersList() {
        List<Order> allOrderList = new ArrayList<>();
        orders.values().forEach(allOrderList::addAll);
        allOrderList.addAll(ordersQueue);
        return allOrderList;
    }

    public void revokeAllOrders() {
        threadConsumer.interrupt();
        getAllOrdersList().forEach(Order::revoke);
        orders.values().forEach(List::clear);
    }

    private boolean filterByClient(Order orderTarget, Order orderCandidate) {
        return orderCandidate.getClient().getId() != orderTarget.getClient().getId();
    }

    private boolean filterByType(Order orderTarget, Order orderCandidate) {
        return !orderCandidate.getOrderType().equals(orderTarget.getOrderType());
    }

    private boolean filterByPrice(Order orderTarget, Order orderCandidate) {
        switch (orderTarget.getOrderType()) {
            case BUY: {
                return orderTarget.getPrice().compareTo(orderCandidate.getPrice()) >= 0;
            }
            case SELL: {
                return orderTarget.getPrice().compareTo(orderCandidate.getPrice()) <= 0;
            }
            default: {
                throw new UnsupportedOrderTypeException();
            }
        }
    }

    private int compareOrdersForSorting(Order order1, Order order2, Order orderTarget) {
        switch (orderTarget.getOrderType()) {
            case BUY: {
                return order1.getPrice().compareTo(order2.getPrice());
            }
            case SELL: {
                return (-1) * order1.getPrice().compareTo(order2.getPrice());
            }
            default: {
                throw new UnsupportedOrderTypeException();
            }
        }
    }

    private BigDecimal getDealPrice(Order orderTarget, Order orderCandidate) {
        switch (orderTarget.getOrderType()) {
            case BUY: {
                return orderTarget.getPrice().min(orderCandidate.getPrice());
            }
            case SELL: {
                return orderTarget.getPrice().max(orderCandidate.getPrice());
            }
            default: {
                throw new UnsupportedOrderTypeException();
            }
        }
    }

    private boolean reduceOrder(Order order, BigDecimal amount, BigDecimal price) {
        order.reduce(amount, price);
        return order.getAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    private void closeOrder(Order order) {
        order.revoke();
        orders.get(order.getCurrencyPair()).remove(order);
        ordersQueue.remove(order);
    }

}
