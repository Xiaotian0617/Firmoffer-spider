package com.ailu.firmoffer.config;

import java.util.Arrays;
import java.util.List;

/**
 * 用于提供一些交易所名称信息
 */
public interface ExchangeName {

    String EX_CHANGE_BITFINEX = "Bitfinex";

    String EX_CHANGE_HUOBI = "Huobi";

    String EX_CHANGE_BIBOX = "Bibox";

    String EX_CHANGE_BINANCE = "Binance";

    String EX_CHANGE_OKEX = "Okex";

    List<String> EXCHANGES = Arrays.asList(EX_CHANGE_BITFINEX, EX_CHANGE_HUOBI, EX_CHANGE_BIBOX, EX_CHANGE_BINANCE, EX_CHANGE_OKEX);
}
