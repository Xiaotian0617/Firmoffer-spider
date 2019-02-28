package com.ailu.firmoffer.config;

import java.util.Arrays;
import java.util.List;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/22 10:35
 */
public class Dic {
    public static final int EX_CHANGE_API_SECRET_KEY_STATUS_NORMAL = 1;
    public static final int EX_CHANGE_API_SECRET_KEY_STATUS_FAILURE = 2;
    public static final int EX_CHANGE_API_BITFINEX_ORDERS_HISTORY_CALL_AMOUNT = 500;
    public static final int EX_CHANGE_API_OKEX_ORDERS_HISTORY_CALL_AMOUNT = 500;

    public static final String FUTURE = "future";
    public static final String STOCK = "stock";
    public static final String WALLET = "wallet";
    public static final String SWAP = "swap";
    public static final String MARGIN = "margin";
    //Okex 期货 全仓模式
    public static final String AllSTORE = "crossed";
    //Okex 期货 逐仓模式
    public static final String LIMITSTORE = "fixed";

    public static final String LONG = "long";
    public static final String SHORT = "short";

    public static final String POSITION = "POSITION";

    public static final String TRANSFER = "TRANSFER";

    public static final String DEPOSIT = "DEPOSIT";

    public static final String LEDGER = "LEDGER";

    public static final String ORDER = "ORDER";

    public static final String MATCH = "MATCH";

    public static final String BLANCE = "BLANCE";

    /**
     * 告诉实盘需要更新
     */
    public static final String UPDATE = "UPDATE";

    /**
     * 告诉实盘需要替换
     */
    public static final String REPLACE = "REPLACE";

    /**
     * 告诉实盘需要新增
     */
    public static final String INSERT = "INSERT";

    /**
     * 告诉实盘需要清空后新增
     */
    public static final String DELETE = "DELETE";

    public static final String SEPARATOR = "-";

    /**
     * 订单未成交
     */
    public static final int EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_NOT_DEAL = 0;
    /**
     * 订单完全成交
     */
    public static final int EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_COMPLETELY_DEAL = 1;
    /**
     * 未完全成交
     */
    public static final int EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_INCOMPLETE_DEAL = 2;
    /**
     * 取消订单
     */
    public static final int EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_CANCEL = 3;
    /**
     * 其他成交状态
     */
    public static final int EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_DEFAULT = 4;
    /**
     * 仓位类型 多仓
     */
    public static final int EX_CHANGE_API_POSITION_TYPE_LONG = 1;
    /**
     * 仓位类型 空仓
     */
    public static final int EX_CHANGE_API_POSITION_TYPE_SHORT = 2;

}
