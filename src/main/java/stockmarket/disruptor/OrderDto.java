package stockmarket.disruptor;

import model.Order;

import java.util.concurrent.CountDownLatch;

public class OrderDto {
    private final CountDownLatch operationCompletedLatch = new CountDownLatch(1);
    private final Order order;

    public OrderDto(Order order) {
        this.order = order;
    }

    public CountDownLatch getOperationCompletedLatch() {
        return operationCompletedLatch;
    }

    public Order getOrder() {
        return order;
    }
}
