package com.ailu.firmoffer.exchange.conversion;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.dao.bean.FirmOfferExchangeBalance;
import com.ailu.firmoffer.dao.bean.FirmOfferExchangeBalanceExample;
import com.ailu.firmoffer.dao.bean.FirmOfferOrderHist;
import com.ailu.firmoffer.dao.bean.FirmOfferPosition;
import com.ailu.firmoffer.domain.CoinContrasts;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.manager.CoinContrastManager;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.bitmex.dto.account.BitmexMarginAccount;
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder;
import org.knowm.xchange.bitmex.dto.trade.BitmexPosition;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.ailu.firmoffer.config.Dic.*;
import static com.ailu.firmoffer.service.MetaService.EX_CHANGE_BITMEX;
import static com.ailu.firmoffer.service.MetaService.EX_CHANGE_OKEX;
import static com.ailu.firmoffer.task.Pending.bitmexOrderHist;

/**
 * NOTE:
 *
 * @Version 1.0
 * @Since JDK1.8
 * @Author mr.wang
 * @Company Henan ailu
 * @Date 2018/12/3 17:57
 */
@Slf4j
@Component
public class BitmexConversion extends ExchangeConversion {
    public void addAccounts(Long userId, List<BitmexMarginAccount> wallets) {
        //获取历史数据
        FirmOfferExchangeBalanceExample balanceExample = new FirmOfferExchangeBalanceExample();
        balanceExample.or().andUserIdEqualTo(userId).andExChangeEqualTo(EX_CHANGE_BITMEX);
        //数据库历史数据
        List<FirmOfferExchangeBalance> balanceListOld = getBlancesMap(userId,Dic.WALLET.toUpperCase());
        //接收的数据
        List<FirmOfferExchangeBalance> balanceListNew = new LinkedList<>();
        //记录改变的数据
        List<String> changeSymbols = new ArrayList<>(50);
        //存储数据
        wallets.forEach(wallet -> {
            FirmOfferExchangeBalance exchangeBalance = new FirmOfferExchangeBalance();
            exchangeBalance.setAmount(wallet.getMarginBalance().divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN));
            exchangeBalance.setExChange(EX_CHANGE_BITMEX);
            //币种
            exchangeBalance.setSymbol("XBT");
            exchangeBalance.setUserId(userId);
            exchangeBalance.setAvailable(wallet.getAvailableMargin().divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN));
            exchangeBalance.setFreeze(wallet.getMarginBalance().divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN));
            exchangeBalance.setLoan(BigDecimal.ZERO);
            exchangeBalance.setType(Dic.WALLET);
            exchangeBalance.setuTime(new Date());

            //存入coinId
            CoinContrasts coin = CoinContrastManager.bitmexContrasts.get("XBT");
            if (coin == null) {
                log.debug("没有找到相应的币种对照表信息，币种 XBT");
            } else {
                exchangeBalance.setCoin(coin.getCoin());
                balanceListNew.add(exchangeBalance);
            }

            if (balanceListOld.size() > 0) {
                //获取数据与历史数据进行对比
                for (FirmOfferExchangeBalance obj : balanceListOld) {
                    //判断与历史数据相等---相等
                    if (obj.getSymbol().equals(exchangeBalance.getSymbol())) {
                        //判断总量变动情况---不相等或者其冻结资产有变化或者可用资产有变化或者借用资产有变化
                        log.info("userId:{} type:{} symbol:{} oldAmount:{} newAmount:{} oldFreeze:{} newFreeze:{} oldVailable:{} newVailable:{} oldLoan:{} newLoan:{}  ", userId, Dic.WALLET, obj.getSymbol(), obj.getAmount(), exchangeBalance.getAmount(), obj.getFreeze(), exchangeBalance.getFreeze()
                                , obj.getAvailable(), exchangeBalance.getAvailable(), obj.getLoan(), exchangeBalance.getLoan());
                        if (checkNeedOrders(exchangeBalance.getAmount(), obj.getAmount())
                                || checkNeedOrders(exchangeBalance.getAvailable(), obj.getAvailable())
                                || checkNeedOrders(exchangeBalance.getFreeze(), obj.getFreeze())) {
                            //余额变动 添加该币种到充值提现
                            changeSymbols.add(exchangeBalance.getSymbol());
                        }
                    }
                }
            } else {
                changeSymbols.add(exchangeBalance.getSymbol());
            }
        });
        if (balanceListNew.isEmpty()) {
            return;
        }
        /*
         * 判断是否触发调用订单查询
         * 1.如果传入历史订单信息不存在，添加一次查询。
         * 2.如果历史信息存在，且如果有余额变动，添加一次查询
         */
        if (balanceListOld.size() > 0) {
            log.info("userId {},type {},变动数据:{}", userId, Dic.WALLET, JSON.toJSONString(balanceListNew));
            //批量更新
            replaceBlance(userId, balanceListNew, EX_CHANGE_BITMEX, Dic.WALLET.toUpperCase(), Dic.DELETE);
            if (changeSymbols.size() > 0) {
                setOrderPending(userId, changeSymbols, Dic.WALLET);
            }
        } else {
            //批量添加
            if (balanceListNew.size() > 0) {
                replaceBlance(userId, balanceListNew, EX_CHANGE_BITMEX, Dic.WALLET.toUpperCase(), Dic.INSERT);
                //历史没有订单记录，现有余额，添加历史订单查询 活跃订单查询
                setOrderPending(userId, changeSymbols, Dic.WALLET);
            }
        }
    }

    private void setOrderPending(Long userId, List<String> symbol, String type) {
        log.debug("Bitmex需要获取订单的数据 userId {} type:{} symbol:{}", userId, type, symbol);
        PendingObj pendingObj = new PendingObj(getOfferKeys(userId), userId, symbol, type);
        synchronized (bitmexOrderHist) {
            bitmexOrderHist.add(pendingObj);
        }
    }


    /**
     * amount freeze available
     * 任一的一个字段，新数据变动幅度超过千分之五，返回true
     *
     * @param newAmount 新数据
     * @param oldAmount 老数据
     * @return
     */
    private boolean checkNeedOrders(BigDecimal newAmount, BigDecimal oldAmount) {
        BigDecimal big = oldAmount.multiply(BigDecimal.ONE.add(new BigDecimal("0.005")));
        BigDecimal small = oldAmount.multiply(BigDecimal.ONE.subtract(new BigDecimal("0.005")));
        return newAmount.compareTo(big) > 0 || newAmount.compareTo(small) < 0;
    }

    /**
     * 增加订单信息
     *
     * @param userId
     * @param futureOrders
     */
    public void addOrders(Long userId, List<BitmexPrivateOrder> futureOrders) {
        List<FirmOfferOrderHist> list = futureOrders.stream()
                //.filter(bitmexPrivateOrder -> bitmexPrivateOrder.getOrderStatus().compareTo(BitmexPrivateOrder.OrderStatus.Filled)==0)
                .map(bitmexPrivateOrder -> {
                    FirmOfferOrderHist firmOfferOrderHist = new FirmOfferOrderHist();
                    firmOfferOrderHist.setId(bitmexPrivateOrder.getId());
                    firmOfferOrderHist.setTradingOn(bitmexPrivateOrder.getSymbol());
                    firmOfferOrderHist.setOrderDate(bitmexPrivateOrder.getTimestamp());
                    firmOfferOrderHist.setExChange(EX_CHANGE_BITMEX);
                    firmOfferOrderHist.setFieldPrice(bitmexPrivateOrder.getAvgPx() != null ? bitmexPrivateOrder.getAvgPx() : BigDecimal.ZERO);
                    firmOfferOrderHist.setOrderStatus(bitmexPrivateOrder.getOrderStatus().ordinal());
                    firmOfferOrderHist.setSide(bitmexPrivateOrder.getSide().name());
                    firmOfferOrderHist.setAmount(bitmexPrivateOrder.getVolume());
                    firmOfferOrderHist.setFieldAmount(bitmexPrivateOrder.getCumQty());
                    firmOfferOrderHist.setPrice(bitmexPrivateOrder.getPrice());
                    firmOfferOrderHist.setUserId(userId);
                    firmOfferOrderHist.setUtime(new Date());
                    firmOfferOrderHist.setType("");
                    return firmOfferOrderHist;
                }).collect(Collectors.toList());
        if (list.isEmpty()) {
            return;
        }
        //ON DUPLICATE KEY UPDATE 储存数据 这里有个逻辑：如果库中这个id的已成交数量有变化才会进行下方的更新操作
        List<FirmOfferOrderHist> insertOrders = getNeedUpdateOrderHists(list, userId);
        if (insertOrders == null || insertOrders.isEmpty()) {
            return;
        }
        replaceOrder(userId, insertOrders, EX_CHANGE_BITMEX, FUTURE, REPLACE);
    }


    public void addPositions(Long userId, List<BitmexPosition> futurePositions) {
        List<FirmOfferPosition> oldPositions = getPositionsMap(userId,FUTURE);
        if (futurePositions == null || futurePositions.isEmpty()) {
            log.info("bitmex userId {} 未抓到存在的持仓,respose:{}", userId, futurePositions);
            if (oldPositions.size() != 0) {
                setOrderPending(userId, Collections.EMPTY_LIST, FUTURE);
            }
            replacePosition(userId, Collections.EMPTY_LIST, EX_CHANGE_OKEX, FUTURE, DELETE);
            return;
        }
        List<FirmOfferPosition> positions = futurePositions.stream().filter(bitmexPosition -> bitmexPosition.getOpen()).map(bitmexPosition -> {
            FirmOfferPosition firmOfferPosition = new FirmOfferPosition();
            firmOfferPosition.setTradingOn(bitmexPosition.getSymbol());
            firmOfferPosition.setInstrumentId(bitmexPosition.getUnderlying());
            firmOfferPosition.setExChange(EX_CHANGE_BITMEX);
            firmOfferPosition.setAvgCost(bitmexPosition.getAvgCostPrice());
            firmOfferPosition.setLeverage(bitmexPosition.getLeverage().toPlainString());
            firmOfferPosition.setQty(bitmexPosition.getCurrentQty().abs());
            firmOfferPosition.setAvailQty(bitmexPosition.getOpeningQty().abs());
            firmOfferPosition.setPnlRatio(bitmexPosition.getUnrealisedRoePcnt().multiply(new BigDecimal("100")));
            firmOfferPosition.setRealizedPnl(bitmexPosition.getRealisedPnl().divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN));
            firmOfferPosition.setMargin(bitmexPosition.getMaintMargin().divide(new BigDecimal("100000000"), 8, BigDecimal.ROUND_HALF_DOWN));
            firmOfferPosition.setMarginMode("");
            firmOfferPosition.setPositionDate(new Date());
            firmOfferPosition.setUserId(userId);
            firmOfferPosition.setUtime(new Date());
            firmOfferPosition.setCurrentCost(bitmexPosition.getCurrentCost());
            firmOfferPosition.setType(BigDecimal.ZERO.compareTo(bitmexPosition.getCurrentQty()) <= 0 ? "long" : "short");
            return firmOfferPosition;
        }).collect(Collectors.toList());
        setOrderPending(userId, Collections.EMPTY_LIST, FUTURE);
        replacePosition(userId, positions, EX_CHANGE_BITMEX, FUTURE, DELETE);
    }
}
