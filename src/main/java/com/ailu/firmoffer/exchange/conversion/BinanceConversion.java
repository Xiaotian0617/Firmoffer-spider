package com.ailu.firmoffer.exchange.conversion;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.dao.bean.FirmOfferExchangeBalance;
import com.ailu.firmoffer.dao.bean.FirmOfferMatchHist;
import com.ailu.firmoffer.domain.CoinContrasts;
import com.ailu.firmoffer.domain.PendingObj;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.*;
import com.binance.api.client.domain.account.request.OrderStatusRequest;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.general.SymbolInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

import static com.ailu.firmoffer.manager.CoinContrastManager.binanceContrasts;
import static com.ailu.firmoffer.service.MetaService.EX_CHANGE_BINANCE;
import static com.ailu.firmoffer.task.Pending.binanceDWH;
import static com.ailu.firmoffer.task.Pending.binanceOrderHist;

/**
 * @Version 1.0
 * @Since JDK1.8
 * @Author HYK
 * @Company 河南艾鹿
 * @Date 2018/5/4 0004 11:13
 */
@Slf4j
@Component
public class BinanceConversion extends ExchangeConversion {


    /**
     * 对查询到的用户账户进行操作
     *
     * @param userId  用户ID
     * @param account 账户信息
     */
    public List<String> balancesApi(long userId, Account account) {
        if (isDisable(account)) {
            log.warn("Binance balances userid {} 未获取到正常数据 {}", userId, account);
            return null;
        } else {
            //判断是否存在历史数据,如果没有，添加。如果存在，更新
            List<FirmOfferExchangeBalance> balanceList = getBlancesMap(userId, Dic.STOCK.toUpperCase());

            // 用来存放格式化后的账户数据
            List<FirmOfferExchangeBalance> list = new LinkedList<>();

            // 用来存放需要进行订单查询的数据(即余额有所变动的数据)
            List<String> symbols = new ArrayList<>(50);
            List<String> symbolsNew = new ArrayList<>(50);

            // 解析查询到的数据并做处理(入库、更新以及触发查询订单和提现记录的操作)
            List<AssetBalance> assetBalanceList = account.getBalances();
            for (AssetBalance assetBalance : assetBalanceList) {
                String symbol = assetBalance.getAsset().toLowerCase();
                FirmOfferExchangeBalance exchangeBalance = new FirmOfferExchangeBalance();
                exchangeBalance.setUserId(userId);
                exchangeBalance.setExChange(EX_CHANGE_BINANCE);
                exchangeBalance.setAvailable(new BigDecimal(assetBalance.getFree()));
                exchangeBalance.setLoan(BigDecimal.ZERO);
                exchangeBalance.setFreeze(new BigDecimal(assetBalance.getLocked()));
                exchangeBalance.setSymbol(assetBalance.getAsset().toLowerCase());
                exchangeBalance.setAmount(exchangeBalance.getAvailable().add(exchangeBalance.getFreeze()));
                exchangeBalance.setuTime(new Date());
                exchangeBalance.setType("stock");

                //存入coinId
                CoinContrasts coin = binanceContrasts.get(symbol.toUpperCase());
                if (coin == null) {
                    log.warn("没有找到相应的币种对照表信息，币种 {} str {}", assetBalance.getAsset(), assetBalance.toString());
                    continue;
                } else {
                    exchangeBalance.setCoin(coin.getCoin());
                }

                //判断此条数据是否有所变动
                for (FirmOfferExchangeBalance firmOfferExchangeBalance : balanceList) {
                    // 首先定位到此条数据
                    if (Objects.equals(firmOfferExchangeBalance.getCoin(), exchangeBalance.getCoin()) &&
                            Objects.equals(firmOfferExchangeBalance.getSymbol(), exchangeBalance.getSymbol())) {
                        // 判断可用余额或者冻结余额是否有变动(有的话需要查询改账户的订单)
                        if (!firmOfferExchangeBalance.getAvailable().setScale(10, BigDecimal.ROUND_DOWN).equals(exchangeBalance.getAvailable().setScale(10, BigDecimal.ROUND_DOWN)) ||
                                !firmOfferExchangeBalance.getFreeze().setScale(10, BigDecimal.ROUND_DOWN).equals(exchangeBalance.getFreeze().setScale(10, BigDecimal.ROUND_DOWN))) {
                            //||   !obj.getFreeze().setScale(10, BigDecimal.ROUND_DOWN).equals(exchangeBalance.getFreeze().setScale(10, BigDecimal.ROUND_DOWN))
                            //余额 ， 冻结 ， 变动 添加该币种到充值提现
                            symbols.add(exchangeBalance.getSymbol());
                        }
                    }
                }

                //如果账户余额以及冻结都不为初始值，则放置到第一次需要初始化的时候的列表中(eg:此处会有一个问题：即如果第一次录入时，用户的资产为0但是是通过交易造成的，则不会去查询与此相关的订单)
                String initBalance = "0.00000000";
                if (!Objects.equals(initBalance, assetBalance.getLocked()) || !Objects.equals(initBalance, assetBalance.getFree())) {
                    symbolsNew.add(exchangeBalance.getSymbol());
                }

                // 添加到需要入库的列表中
                list.add(exchangeBalance);
            }

            // 如果之前有此交易所此用户的相关账户信息,则只需要进行更新操作
            if (balanceList.size() > 0) {
                // 更新账户信息
                replaceBlance(userId, list, EX_CHANGE_BINANCE, Dic.STOCK, Dic.DELETE);
                // 判断是否有账户的余额变动信息(以进行订单的查询操作)
                if (symbols.size() > 0) {
                    // 对这些账户信息有所变动的仓位进行订单的查询
                    setOrderPending(userId, symbols);
                }
                return symbols;
            } else {
                // 之前没有此账户此交易所的信息并且此次查询有此账户此交易所的信息
                if (list.size() > 0) {
                    // 插入账户信息
                    replaceBlance(userId, list, EX_CHANGE_BINANCE, Dic.STOCK, Dic.INSERT);
                    // 历史没有订单记录，现有余额，添加历史订单查询 活跃订单查询(此处设置只有账户可用余额或者冻结余额不为0.0000000的需要进行订单查询)
                    setOrderPending(userId, symbolsNew);
                }
                return symbolsNew;
            }

        }
    }

    /**
     * 判断账户余额信息是否不可用
     *
     * @param account 账户余额信息
     * @return 是否不可用(不可用 : true ; 可用 : false)
     */
    private boolean isDisable(Account account) {
        if (Objects.equals(null, account)) {
            return true;
        }
        return false;
    }

    /**
     * 设置需要进行订单查询和提现及充值查询
     *
     * @param userId 用户ID
     * @param symbol 需要进行查询的仓位(BTC,ETH...)
     */
    private void setOrderPending(Long userId, List<String> symbol) {
        binanceOrderHist.add(new PendingObj(getOfferKeys().get(userId), userId, symbol));
        binanceDWH.add(new PendingObj(getOfferKeys().get(userId), userId, symbol));
    }

    /**
     * 查询订单详情
     *
     * @param userId  用户ID
     * @param client  api client
     * @param symbols 所有有变动的交易所
     */
    public void matchsHistApi(long userId, BinanceApiRestClient client, List<String> symbols) {
        // 需要存入数据库的列表
        List<FirmOfferMatchHist> list = new LinkedList<>();


        // 获取交易所支持的所有交易对
        ExchangeInfo exchangeInfo = client.getExchangeInfo();
        List<SymbolInfo> symbolList = exchangeInfo.getSymbols();


        // 获取需要进行订单查询的交易对
        symbolList = needSelectList(symbols, symbolList);

        // 遍历交易对
        for (SymbolInfo symbolInfo : symbolList) {
            String symbol = symbolInfo.getSymbol();

            // 获取此交易对的相应订单
            try {
                List<Trade> myTradeList = client.getMyTrades(symbol);
                for (Trade myTrade : myTradeList) {
                    // 获取订单详细信息
                    Order order = client.getOrderStatus(new OrderStatusRequest(symbol, Long.valueOf(myTrade.getOrderId())));
                    FirmOfferMatchHist firmOfferMatchHist = new FirmOfferMatchHist();
                    firmOfferMatchHist.setId(order.getOrderId());
                    firmOfferMatchHist.setUserId(userId);
                    firmOfferMatchHist.setSource((order.getStatus() + "").toLowerCase());
                    firmOfferMatchHist.setExChange(EX_CHANGE_BINANCE);
                    firmOfferMatchHist.setSymbol(order.getSymbol().toLowerCase());
                    firmOfferMatchHist.setMatchId(order.getOrderId());
                    firmOfferMatchHist.setType((order.getSide() + "-" + order.getType()).toLowerCase());
                    firmOfferMatchHist.setPrice(new BigDecimal(myTrade.getPrice()));
                    firmOfferMatchHist.setFieldAmount(new BigDecimal(order.getExecutedQty()));
                    firmOfferMatchHist.setFieldPrice(new BigDecimal(myTrade.getPrice()));
                    firmOfferMatchHist.setMatchDate(new Date(order.getTime()));
                    firmOfferMatchHist.setFieldFees(new BigDecimal(myTrade.getCommission()));
                    firmOfferMatchHist.setMatchType(Dic.STOCK);
                    list.add(firmOfferMatchHist);
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                log.error("binance order errror", e);
            }
        }

        // 存入数据库
        if (list.size() > 0) {
            replaceMatch(userId, list, EX_CHANGE_BINANCE, Dic.STOCK, Dic.REPLACE);
        }
    }

    /**
     * 获取需要进行订单查询的交易对
     *
     * @param symbols    需要进行查询的仓位
     * @param symbolList 所有交易对
     * @return 需要进行订单查询的交易对
     */
    private List<SymbolInfo> needSelectList(List<String> symbols, List<SymbolInfo> symbolList) {
        List<SymbolInfo> symbolInfoList = new ArrayList<>();

        // 遍历两个集合,获取其交集点
        for (SymbolInfo symbolInfo : symbolList) {
            for (String symbol : symbols) {
                // 如果此交易对中包含此仓位,则放置进需要进行订单查询的交易对列表中,并进行下一个交易对的判断
                if (!symbolInfo.getQuoteAsset().equalsIgnoreCase(symbol) && symbolInfo.getBaseAsset().equalsIgnoreCase(symbol)) {
                    symbolInfoList.add(symbolInfo);
                    continue;
                }
            }
        }

        return symbolInfoList;
    }

    /**
     * 存取款历史纪录
     * @param userId
     * @param withdrawHistory
     * @param depositHistory
     */
//    public void depositWithdrawalHistory(long userId, WithdrawHistory withdrawHistory, DepositHistory depositHistory) {
//        if (isDisable(withdrawHistory)) {
//            log.warn("binance withdrawalHistory userid {} 未获取到正常数据 {}", userId, withdrawHistory);
//        } else {
//            List<Withdraw> withdrawList = withdrawHistory.getWithdrawList();
//            // 需要进行存储的数据
//            List<FirmOfferMovements> list = new LinkedList<>();
//            withdrawList.forEach(withdraw -> {
//                FirmOfferMovements movements = new FirmOfferMovements();
//                movements.setId(EX_CHANGE_BINANCE + movements.getId());
//                movements.setType("withdraw");
//                movements.setCurrency(withdraw.getAsset());
//                movements.setAmount(new BigDecimal(withdraw.getAmount()));
//                movements.setAddress(withdraw.getAddress());
//                movements.setFee(BigDecimal.ZERO);
//                movements.setState(String.valueOf(withdraw.getStatus()));
//                movements.setCreatedTime(new Date(Long.valueOf(withdraw.getApplyTime())));
//                movements.setUpdatedTime(new Date(Long.valueOf(withdraw.getSuccessTime())));
//                movements.setTxid(withdraw.getTxId());
//                movements.setUserId(userId);
//                movements.setExChange(EX_CHANGE_BINANCE);
//                list.add(movements);
//            });
//
//            //ON DUPLICATE KEY UPDATE 储存数据
//            if (list.size() > 0) {
//                movementsExt.insertDuplicateUpdate(list);
//            }
//        }
//        if (isDisable(depositHistory)) {
//            log.warn("binance depositHistory userid {} 未获取到正常数据 {}", userId, depositHistory);
//        } else {
//            List<Deposit> depositList = depositHistory.getDepositList();
//            // 需要进行存储的数据
//            List<FirmOfferMovements> list = new LinkedList<>();
//            depositList.forEach(deposit -> {
//                FirmOfferMovements movements = new FirmOfferMovements();
//                movements.setId(EX_CHANGE_BINANCE + movements.getId());
//                movements.setType("deposit");
//                movements.setCurrency(deposit.getAsset());
//                movements.setAmount(new BigDecimal(deposit.getAmount()));
//                movements.setAddress("");
//                movements.setFee(BigDecimal.ZERO);
//                movements.setState(String.valueOf(deposit.getStatus()));
//                movements.setCreatedTime(new Date(Long.valueOf(deposit.getInsertTime())));
//                movements.setUpdatedTime(new Date(Long.valueOf(deposit.getInsertTime())));
//                movements.setTxid(deposit.getTxId());
//                movements.setUserId(userId);
//                movements.setExChange(EX_CHANGE_BINANCE);
//                list.add(movements);
//            });
//
//            //ON DUPLICATE KEY UPDATE 储存数据
//            if (list.size() > 0) {
//                movementsExt.insertDuplicateUpdate(list);
//            }
//        }
//    }

    /**
     * 判断账户提现信息是否不可用
     *
     * @param withdrawHistory 账户提现信息
     * @return 是否不可用(不可用 : true ; 可用 : false)
     */
    private boolean isDisable(WithdrawHistory withdrawHistory) {
        if (Objects.equals(null, withdrawHistory)) {
            return true;
        }
        return false;
    }

    /**
     * 判断账户充值信息是否不可用
     *
     * @param depositHistory 账户充值信息
     * @return 是否不可用(不可用 : true ; 可用 : false)
     */
    private boolean isDisable(DepositHistory depositHistory) {
        if (Objects.equals(null, depositHistory)) {
            return true;
        }
        return false;
    }

//    public List<LinkedHashMap> getBinanceAccountsIdMap(Long userId) {
//        return binanceAccountsIdMap.getOrDefault(userId,Collections.EMPTY_LIST);
//    }

}
