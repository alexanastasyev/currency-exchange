package stockmarket.disruptor;

public class OrderEvent {
    private OrderDto order;

    public OrderDto getOrder() {
        return order;
    }

    public void setOrder(OrderDto order) {
        this.order = order;
    }
}
