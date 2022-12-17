package stockmarket.disruptor;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import exception.UnsupportedOrderTypeException;
import model.CurrencyPair;
import model.Order;
import stockmarket.StockMarket;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
public class StockMarketDisruptor implements StockMarket {

    private static final int BUFFER_SIZE = 2048;

    private final Map<CurrencyPair, List<Order>> orders;

    public Disruptor<OrderEvent> disruptor = new Disruptor<>(OrderEvent::new, BUFFER_SIZE, DaemonThreadFactory.INSTANCE);

    public StockMarketDisruptor() {
        this.orders = new ConcurrentHashMap<>(CurrencyPair.values().length);
        for (CurrencyPair currencyPair : CurrencyPair.values()) {
            orders.put(currencyPair, new ArrayList<>());
        }

        disruptor.handleEventsWith((event, sequence, endOfBatch) -> matchOrder(event.getOrder()));
        disruptor.start();
    }

    @Override
    public void addOrder(Order order) {
        OrderDto orderDto = new OrderDto(order);
        disruptor.getRingBuffer().publishEvent((event, sequence, endOfBatch) -> event.setOrder(orderDto));
        try {
            orderDto.getOperationCompletedLatch().await();
        } catch (InterruptedException e) {
            System.out.println("Interrupted");
            disruptor.shutdown();
        }
    }

    @Override
    public List<Order> getAllOrdersList() {
        Set<Order> allOrdersSet = new HashSet<>();
        orders.values().forEach(allOrdersSet::addAll);
        return new ArrayList<>(allOrdersSet);
    }

    @Override
    public void revokeAllOrders() {
        getAllOrdersList().forEach(Order::revoke);
        orders.values().forEach(List::clear);
    }

    private void matchOrder(OrderDto orderDto) {
        List<Order> orderCandidates = orders.get(orderDto.getOrder().getCurrencyPair()).stream()
                .filter(orderCandidate -> filterByClient(orderDto.getOrder(), orderCandidate))
                .filter(orderCandidate -> filterByType(orderDto.getOrder(), orderCandidate))
                .filter(orderCandidate -> filterByPrice(orderDto.getOrder(), orderCandidate))
                .sorted((order1, order2) -> compareOrdersForSorting(order1, order2, orderDto.getOrder()))
                .collect(Collectors.toList());

        orderCandidates.forEach(orderCandidate -> {
            BigDecimal dealAmount = orderCandidate.getAmount().min(orderDto.getOrder().getAmount());
            BigDecimal dealPrice = getDealPrice(orderDto.getOrder(), orderCandidate);

            if (reduceOrder(orderCandidate, dealAmount, dealPrice)) {
                closeOrder(orderCandidate);
            }
            if (reduceOrder(orderDto.getOrder(), dealAmount, dealPrice)) {
                orderDto.getOrder().revoke();
            }
        });

        if (orderDto.getOrder().getAmount().compareTo(BigDecimal.ZERO) > 0) {
            orders.get(orderDto.getOrder().getCurrencyPair()).add(orderDto.getOrder());
        }

        orderDto.getOperationCompletedLatch().countDown();
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
    }
}
