package model;

import exception.NotEnoughMoneyException;
import util.CurrencyUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    private final int id;
    private final Map<Currency, BigDecimal> balance;

    public Client(int id) {
        this.id = id;

        Map<Currency, BigDecimal> balance = new ConcurrentHashMap<>(Currency.values().length);
        for (Currency currency : Currency.values()) {
            balance.put(currency, BigDecimal.ZERO.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE));
        }
        this.balance = balance;
    }

    public void deposit(Currency currency, BigDecimal amount) {
        synchronized (currency) {
            BigDecimal currentAmount = balance.get(currency);
            BigDecimal newAmount = currentAmount.add(amount).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
            balance.put(currency, newAmount);
        }
    }

    public void withdraw(Currency currency, BigDecimal amount) {
        synchronized (currency) {
            BigDecimal currentAmount = balance.get(currency);
            if (currentAmount.compareTo(amount.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE)) == -1) {
                throw new NotEnoughMoneyException(String.format("Trying to withdraw %s %s, but the client %s has only %s", amount.setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE), currency, this.getId(), currentAmount));
            }
            BigDecimal newAmount = currentAmount.subtract(amount).setScale(CurrencyUtils.SCALE, CurrencyUtils.ROUNDING_MODE);
            balance.put(currency, newAmount);
        }
    }

    public int getId() {
        return id;
    }

    public Map<Currency, BigDecimal> getBalance() {
        return new HashMap<>(this.balance);
    }

}
