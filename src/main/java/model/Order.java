package model;

import exception.NotEnoughMoneyException;
import util.CurrencyUtils;

import java.math.BigDecimal;
import java.util.Objects;

public class Order {
    private final Client client;
    private final CurrencyPair currencyPair;
    private final OrderType orderType;
    private BigDecimal amount;
    private BigDecimal deposit;
    private final BigDecimal price;

    public Order(Client client, CurrencyPair currencyPair, OrderType orderType, BigDecimal amount, BigDecimal price) {
        switch (orderType) {
            case BUY: {
                BigDecimal clientHasMoney = client.getBalance().get(currencyPair.getSecondCurrency());
                BigDecimal needMoney = price.multiply(amount).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
                if (needMoney.compareTo(clientHasMoney) > 0) {
                    throw new NotEnoughMoneyException(String.format("Cannot create order. Needed at least %s %s. Client %s has only %s", needMoney, currencyPair.getSecondCurrency(), client.getId(), clientHasMoney));
                }
                client.withdraw(currencyPair.getSecondCurrency(), needMoney);
                this.deposit = needMoney.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
                break;
            }

            case SELL: {
                BigDecimal clientHasMoney = client.getBalance().get(currencyPair.getFirstCurrency());
                BigDecimal needMoney = amount.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
                if (needMoney.compareTo(clientHasMoney) > 0) {
                    throw new NotEnoughMoneyException(String.format("Cannot create order. Needed at least %s %s. Client %s has only %s", needMoney, currencyPair.getFirstCurrency(), client.getId(), clientHasMoney));
                }
                client.withdraw(currencyPair.getFirstCurrency(), needMoney);
                this.deposit = BigDecimal.ZERO;
                break;
            }
        }

        this.client = client;
        this.currencyPair = currencyPair;
        this.orderType = orderType;
        this.amount = amount.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
        this.price = price.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
    }

    public void reduce(BigDecimal amount, BigDecimal price) {
        if (this.amount.compareTo(amount.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE)) < 0) {
            throw new NotEnoughMoneyException(String.format("Cannot withdraw order for %s. Current amount is %s", amount, this.amount));
        }

        BigDecimal newAmount = this.amount.subtract(amount).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
        this.amount = newAmount;

        BigDecimal dealPrice = amount.multiply(price).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);

        switch (orderType) {
            case BUY: {
                client.deposit(currencyPair.getFirstCurrency(), amount);
                BigDecimal newDeposit = deposit.subtract(dealPrice);
                this.deposit = newDeposit;
                break;
            }
            case SELL: {
                client.deposit(currencyPair.getSecondCurrency(), dealPrice);
                break;
            }
        }
    }

    public void revoke() {
        switch (orderType) {
            case BUY: {
                if (deposit.compareTo(BigDecimal.ZERO) > 0) {
                    client.deposit(currencyPair.getSecondCurrency(), deposit);
                    this.deposit = BigDecimal.ZERO;
                }
                break;
            }
            case SELL: {
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    client.deposit(currencyPair.getFirstCurrency(), amount);
                }
                break;
            }
        }
    }

    public Client getClient() {
        return client;
    }

    public CurrencyPair getCurrencyPair() {
        return currencyPair;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(client, order.client) && currencyPair == order.currencyPair && orderType == order.orderType && Objects.equals(amount, order.amount) && Objects.equals(deposit, order.deposit) && Objects.equals(price, order.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, currencyPair, orderType, amount, deposit, price);
    }
}
