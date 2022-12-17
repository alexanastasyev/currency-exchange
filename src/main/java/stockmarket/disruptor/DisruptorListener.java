package stockmarket.disruptor;

import com.lmax.disruptor.EventHandler;
import model.CurrencyPair;
import model.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DisruptorListener implements EventHandler<OrderEvent> {

    private final CurrencyPair currencyPair;

    private final List<Order> orders;
    private final Consumer<Order> processor;

    public DisruptorListener(CurrencyPair currencyPair, Consumer<Order> processor) {
        this.currencyPair = currencyPair;
        this.processor = processor;
        this.orders = new ArrayList<>();
    }

    @Override
    public void onEvent(OrderEvent event, long sequence, boolean endOfBatch) throws Exception {
        OrderDto orderDto = event.getOrder();
        Order order = orderDto.getOrder();
        processor.accept(order);
        orderDto.getOperationCompletedLatch().countDown();
    }
}
