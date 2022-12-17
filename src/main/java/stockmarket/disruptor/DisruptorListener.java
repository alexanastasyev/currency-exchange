package stockmarket.disruptor;

import com.lmax.disruptor.EventHandler;
import exception.UnsupportedOrderTypeException;
import model.CurrencyPair;
import model.Order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("DuplicatedCode")
public class DisruptorListener implements EventHandler<OrderEvent> {

    private final CurrencyPair currencyPair;

    private final List<Order> orders;

    public DisruptorListener(CurrencyPair currencyPair) {
        this.currencyPair = currencyPair;
        this.orders = new ArrayList<>();
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) {
        OrderDto orderDto = event.getOrder();
        Order order = orderDto.getOrder();
        if (!order.getCurrencyPair().equals(currencyPair)) {
            return;
        }
        matchOrder(order);
        orderDto.getOperationCompletedLatch().countDown();
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders);
    }

    public void revokeAllOrders() {
        orders.forEach(Order::revoke);
        orders.clear();
    }

    private void matchOrder(Order order) {
        List<Order> orderCandidates = orders.stream()
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
            orders.add(order);
        }
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
        orders.remove(order);
    }
}
