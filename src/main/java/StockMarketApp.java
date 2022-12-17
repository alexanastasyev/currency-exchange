import com.google.gson.Gson;
import model.*;
import stockmarket.StockMarket;
import stockmarket.StockMarketSimple;

import java.math.BigDecimal;

public class StockMarketApp {

    private static final Gson gson = new Gson();

    public static void main(String[] args) {

        Client client = new Client(1);
        client.deposit(Currency.RUB, new BigDecimal(1000.62));
        Order order = new Order(client, CurrencyPair.USD_RUB, OrderType.BUY, new BigDecimal(10), new BigDecimal(61.50));

        StockMarket stockMarket = new StockMarketSimple();
        stockMarket.addOrder(order);

        System.out.println(gson.toJson(stockMarket.getAllOrdersList()));
    }
}
