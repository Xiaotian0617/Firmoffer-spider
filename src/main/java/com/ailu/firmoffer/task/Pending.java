package com.ailu.firmoffer.task;

import com.ailu.firmoffer.domain.PendingObj;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/26 14:39
 */
public class Pending {
    /**
     * bitfinex历史订单
     **/
    public static CopyOnWriteArrayList<PendingObj> bitfinexOrderHist = new CopyOnWriteArrayList<>();
    /**
     * bitfinex活跃订单
     **/
    public static CopyOnWriteArrayList<PendingObj> bitfinexActiveHist = new CopyOnWriteArrayList<>();
    /**
     * bitfinex充值提现
     **/
    public static CopyOnWriteArrayList<PendingObj> bitfinDWH = new CopyOnWriteArrayList<>();

    /**
     * huobi订单
     **/
    public static Set<PendingObj> huobiOrderHist = Collections.synchronizedSet(new HashSet<>());

    /**
     * huobi订单
     **/
    public static Set<PendingObj> huobiMatchHist = Collections.synchronizedSet(new HashSet<>());

    /**
     * huobi订单转Map
     */
    public static Map<Long, PendingObj> huobiPendingObjMap = new ConcurrentHashMap<>();

    /**
     * huobi充值提现
     **/
    public static Set<PendingObj> huobiDWH = Collections.synchronizedSet(new HashSet<>());

    /**
     * okex历史订单
     **/
    public static Set<PendingObj> okexOrderHist = Collections.synchronizedSet(new HashSet<>());
    /**
     * okex活跃订单
     **/
    public static Set<PendingObj> okexActiveHist = Collections.synchronizedSet(new HashSet<>());
    /**
     * okex充值提现
     **/
    public static Set<PendingObj> okexDepositHist = Collections.synchronizedSet(new HashSet<>());
    /**
     * okex账单流水
     **/
    public static Set<PendingObj> okexLedgerHist = Collections.synchronizedSet(new HashSet<>());
    /**
     * okex转账
     **/
    public static Set<PendingObj> okexTransferHist = Collections.synchronizedSet(new HashSet<>());

    /**
     * Bitmex历史订单
     **/
    public static Set<PendingObj> bitmexOrderHist = Collections.synchronizedSet(new HashSet<>());

    /**
     * binance订单
     **/
    public static Set<PendingObj> binanceOrderHist = Collections.synchronizedSet(new HashSet<>());
    /**
     * binance充值提现
     **/
    public static Set<PendingObj> binanceDWH = Collections.synchronizedSet(new HashSet<>());

    /**
     * bibox历史订单
     **/
    public static Set<PendingObj> biboxOrderHist = Collections.synchronizedSet(new HashSet<>());
    /**
     * bibox活跃订单
     **/
    public static Set<PendingObj> biboxActiveHist = Collections.synchronizedSet(new HashSet<>());

}
