package stockmarket.disruptor;

import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import model.CurrencyPair;
import model.Order;
import stockmarket.StockMarket;

import java.util.*;

@SuppressWarnings("DuplicatedCode")
public class StockMarketDisruptor implements StockMarket {

    private static final int BUFFER_SIZE = 2048;

    private final Disruptor<OrderEvent> disruptor;
    private final Map<CurrencyPair, DisruptorListener> pairListeners = new HashMap<>();

    public StockMarketDisruptor() {
        disruptor = new Disruptor<>(OrderEvent::new, BUFFER_SIZE, DaemonThreadFactory.INSTANCE);
        for (CurrencyPair currencyPair : CurrencyPair.values()) {
            DisruptorListener listener = new DisruptorListener(currencyPair);
            pairListeners.put(currencyPair, listener);
            disruptor.handleEventsWith(listener);
        }
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
        pairListeners.values().forEach(listener -> allOrdersSet.addAll(listener.getAllOrders()));
        return new ArrayList<>(allOrdersSet);
    }

    @Override
    public void revokeAllOrders() {
        pairListeners.values().forEach(DisruptorListener::revokeAllOrders);
    }
}
