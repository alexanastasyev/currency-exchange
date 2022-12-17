package stockmarket;

import model.Order;

import java.util.List;

public interface StockMarket {
    void addOrder(Order order);
    List<Order> getAllOrdersList();
    void revokeAllOrders();
}
