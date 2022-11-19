package model;

/**
 * List of supported currency pairs
 */
public enum CurrencyPair {
    USD_EUR(Currency.USD, Currency.EUR),
    USD_RUB(Currency.USD, Currency.RUB),
    USD_JPY(Currency.USD, Currency.JPY),
    USD_GBP(Currency.USD, Currency.GBP),
    USD_AUD(Currency.USD, Currency.AUD),
    USD_CAD(Currency.USD, Currency.CAD),
    USD_CHF(Currency.USD, Currency.CHF),
    USD_SEK(Currency.USD, Currency.SEK),
    EUR_RUB(Currency.EUR, Currency.RUB),
    EUR_GBP(Currency.EUR, Currency.GBP),
    EUR_CHF(Currency.EUR, Currency.CHF),
    RUB_JPY(Currency.RUB, Currency.JPY);

    private final Currency firstCurrency;
    private final Currency secondCurrency;

    CurrencyPair(Currency firstCurrency, Currency secondCurrency) {
        this.firstCurrency = firstCurrency;
        this.secondCurrency = secondCurrency;
    }

    public Currency getFirstCurrency() {
        return firstCurrency;
    }

    public Currency getSecondCurrency() {
        return secondCurrency;
    }
}
