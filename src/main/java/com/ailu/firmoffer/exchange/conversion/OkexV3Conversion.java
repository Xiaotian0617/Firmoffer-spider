package com.ailu.firmoffer.exchange.conversion;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.dao.bean.*;
import com.ailu.firmoffer.domain.CoinContrasts;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.manager.CoinContrastManager;
import com.ailu.firmoffer.util.ContractUtil;
import com.ailu.firmoffer.util.DateUtils;
import com.ailu.firmoffer.util.SendKafkaUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.okcoin.okex.open.api.bean.account.result.Ledger;
import com.okcoin.okex.open.api.bean.account.result.Wallet;
import com.okcoin.okex.open.api.bean.spot.result.Account;
import com.okcoin.okex.open.api.bean.spot.result.OrderInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ailu.firmoffer.config.Dic.*;
import static com.ailu.firmoffer.service.MetaService.EX_CHANGE_OKEX;
import static com.ailu.firmoffer.task.Pending.*;

/**
 * @Description:
 * @author: mr.wang
 * @version: V1.0
 * @date: 2018年11月9日15:42:50
 */
@Slf4j
@Component
public class OkexV3Conversion extends ExchangeConversion {

    @Resource
    private ContractUtil contractUtil;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    SendKafkaUtils sendKafkaUtils;

    private static final String EX_CHANGE_API_OKEX_ORIGIN_DISABLE = "error";
    private static final String EX_CHANGE_API_OKEX_ORIGIN_EMPTY = "[]";

    /**
     * 存储余额总额以及其他
     *
     * @param userId
     * @param accounts
     */
    public void addStockBalances(Long userId, List<Account> accounts) {
        if (accounts.isEmpty()) {
            log.warn("userId {}  该用户没有现货账户", userId);
            return;
        }
        //获取现货总量
        Map<String, Account> stockMap = accounts.stream().collect(Collectors.toMap(Account::getCurrency, Function.identity()));
        if (stockMap != null) {
            addOkexBalanceInfo(userId, stockMap, Dic.STOCK);
        }
    }

    /**
     * 存储余额总额以及其他
     *
     * @param userId
     * @param jsonObject
     */
    public void addFutureBalances(Long userId, JSONObject jsonObject) {
//        if (!jsonObject.getBoolean("result")) {
//            log.debug("userId {}  该用户没有合约账户或请求出错！", userId);
//        }
        //获取合约总量
        Map<String, Account> futureMap = getbalancesFuture(userId, jsonObject);
        if (futureMap != null) {
            addOkexBalanceInfo(userId, futureMap, Dic.FUTURE);
        }
    }

    /**
     * 增加用户合约资产
     *
     * @param userId
     * @param swapResult
     */
    public void addSwapBalances(Long userId, JSONObject swapResult) {
        if (swapResult == null || swapResult.getJSONArray("info") == null || swapResult.getJSONArray("info").isEmpty()) {
            log.warn("userId {}  该用户没有永续合约账户或请求出错", userId);
            return;
        }
        //获取现货总量
        Map<String, Account> swapMap = getBalancesSwap(userId, swapResult);
        if (swapMap != null) {
            addOkexBalanceInfo(userId, swapMap, SWAP);
        }
    }

    /**
     * 增加用户杠杆资产
     * 总资产等于 各个资产和 - 各个资产借用 - 各个资产利息
     * [
     * {
     * "currency:BTC": {
     * "available": "0",
     * "balance": "0",
     * "borrowed": "0",
     * "frozen": "0",
     * "hold": "0",
     * "holds": "0",
     * "lending_fee": "0"
     * },
     * "currency:LTC": {
     * "available": "0",
     * "balance": "0",
     * "borrowed": "0",
     * "frozen": "0",
     * "hold": "0",
     * "holds": "0",
     * "lending_fee": "0"
     * },
     * "instrument_id": "LTC-BTC",
     * "liquidation_price": "0",
     * "product_id": "LTC-BTC",
     * "risk_rate": ""
     * }
     * ]
     *
     * @param userId
     * @param marginResult
     */
    public void addMarginBalances(Long userId, List<Map<String, Object>> marginResult) {
        if (marginResult == null || marginResult.isEmpty()) {
            log.warn("userId {} 该用户没有杠杆资产或请求出错！", userId);
            return;
        }
        //获取合约总量
        Map<String, Account> marginMap = getbalancesMargin(userId, marginResult);
        if (marginMap != null) {
            addOkexBalanceInfo(userId, marginMap, MARGIN);
        }
    }

    private Map<String, Account> getbalancesMargin(Long userId, List<Map<String, Object>> marginResult) {
        Map<String, Account> symbolVolMap = new HashMap<>();
        marginResult.forEach(map -> {
            Object instrument_id = map.get("instrument_id");
            if (instrument_id == null) {
                log.error("用户id {} margin 资产数据有误 instrument_id 为null", userId);
                return;
            }
            String[] symbols = instrument_id.toString().split("-");
            if (symbols.length != 2) {
                log.error("用户id {} margin 资产数据有误 symbols.length!=2 ", userId);
                return;
            }
            String o1 = JSON.toJSONString(map.get("currency:" + symbols[0]));
            String o2 = JSON.toJSONString(map.get("currency:" + symbols[1]));
            if (o1 == null || o2 == null) {
                log.error("用户id {} margin 资产数据有误 o1 或 o2 为 null ", userId);
                return;
            }
            AccountTemp symbol1 = JSONObject.parseObject(o1, AccountTemp.class);
            AccountTemp symbol2 = JSONObject.parseObject(o2, AccountTemp.class);
            putMapBySymbol(symbols[0], symbol1, symbolVolMap);
            putMapBySymbol(symbols[1], symbol2, symbolVolMap);
        });
        if (symbolVolMap.isEmpty()) {
            return null;
        }
        return symbolVolMap;
    }

    @Data
    static class AccountTemp {
        private String id;
        private String currency;
        private String balance;
        private String available;
        private String hold;
        @JSONField(name = "lending_fee")
        private String fee;
        private String borrowed;
    }

    /**
     * 增加 marginCurrency 到 map
     * 1.先判断map中是否有对应币种的值 没有则直接增加
     * 2.如果有 需要将之前的拿出 并逐个数据进行相加
     *
     * @param symbol
     * @param account
     * @param accountMap
     */
    private void putMapBySymbol(String symbol, AccountTemp account, Map<String, Account> accountMap) {
        Account mapValue = accountMap.get(symbol);
        if (mapValue == null) {
            mapValue = new Account();
            BeanUtils.copyProperties(account, mapValue);
            mapValue.setCurrency(symbol);
            accountMap.put(symbol, mapValue);
            return;
        }
        mapValue.setAvailable(new BigDecimal(mapValue.getAvailable()).add(new BigDecimal(account.getAvailable())).toPlainString());
        mapValue.setBalance(new BigDecimal(mapValue.getBalance()).add(new BigDecimal(account.getBalance())).toPlainString());
        mapValue.setBorrowed(new BigDecimal(mapValue.getBorrowed()).add(new BigDecimal(account.getBorrowed())).toPlainString());
        mapValue.setHold(new BigDecimal(mapValue.getHold()).add(new BigDecimal(account.getHold())).toPlainString());
        mapValue.setFee(new BigDecimal(mapValue.getFee()).add(new BigDecimal(account.getFee())).toPlainString());
        mapValue.setCurrency(symbol);
        accountMap.put(symbol, mapValue);
    }

    private Map<String, Account> getBalancesSwap(Long userId, JSONObject swapResult) {
        Map<String, Account> accounts = new HashMap<>(200);
        swapResult.getJSONArray("info").forEach(array -> {
            Account account = new Account();
            JSONObject job = (JSONObject) array;
            //Okex 期货分全仓和逐仓 这里需区分下
            String marginMode = job.getString("margin_mode");
            if (FUTURECROSSED.equalsIgnoreCase(marginMode)) {
                log.debug("userId {} 是全仓模式", userId);
                //全仓模式
                if (job.getString("instrument_id") == null) {
                    return;
                }
                account.setCurrency(job.getString("instrument_id").split("-")[0]);
                account.setAvailable(job.getString("total_avail_balance"));
                account.setBalance(job.getString("equity"));
                account.setHold(job.getString("margin"));
                accounts.put(account.getCurrency(), account);
                return;
            } else if (FUTUREFIXED.equalsIgnoreCase(marginMode)) {
                log.debug("userId {} 是逐仓仓模式", userId);
                account.setCurrency(job.getString("instrument_id").split("-")[0]);
                account.setAvailable(job.getString("fixed_balance"));
                account.setBalance(job.getString("equity"));
                account.setHold(job.getString("margin"));
                accounts.put(account.getCurrency(), account);
                return;
            }
            log.info("userId {} 是未知模式已跳过", userId);
        });
        return accounts;
    }

    /**
     * 存储钱包总额
     *
     * @param userId
     * @param walletResults
     */
    public void addWalletBalances(Long userId, List<Wallet> walletResults) {
        if (walletResults == null || walletResults.isEmpty()) {
            log.debug("userId {}  获取该用户钱包信息有误，稍后重试！", userId);
            return;
        }
        //获取现货总量
        Map<String, Account> walletMap = walletResults.stream().map(wallet -> coverWalletToAccount(wallet)).collect(Collectors.toMap(Account::getCurrency, Function.identity()));
        if (walletMap != null) {
            addOkexBalanceInfo(userId, walletMap, Dic.WALLET);
        }
    }

    private Account coverWalletToAccount(Wallet wallet) {
        Account account = new Account();
        account.setBalance(wallet.getBalance().toEngineeringString());
        account.setAvailable(wallet.getAvailable().toEngineeringString());
        account.setCurrency(wallet.getCurrency());
        account.setHold(wallet.getFrozen() == null ? "0" : wallet.getFrozen().toEngineeringString());
        return account;
    }

    private void addOkexBalanceInfo(Long userId, Map<String, Account> map, String type) {
        //数据库历史数据
        List<FirmOfferExchangeBalance> balanceListOld = getBlancesMap(userId, type.toUpperCase());
        if (map.isEmpty()){
            if (!balanceListOld.isEmpty()){
                setOrderPending(userId, balanceListOld.stream().map(FirmOfferExchangeBalance::getSymbol).collect(Collectors.toList()), type);
            }
            replaceBlance(userId, balanceListOld, EX_CHANGE_OKEX, type.toUpperCase(), Dic.DELETE);
            return;
        }
        //接收的数据
        List<FirmOfferExchangeBalance> balanceListNew = new LinkedList<>();
        //记录改变的数据
        List<String> changeSymbols = new ArrayList<>(50);
        //存储数据
        map.forEach((coinname, account) -> {
            FirmOfferExchangeBalance exchangeBalance = new FirmOfferExchangeBalance();
            exchangeBalance.setAmount(new BigDecimal(account.getBalance()));
            exchangeBalance.setExChange(EX_CHANGE_OKEX);
            //币种
            exchangeBalance.setSymbol(coinname);
            exchangeBalance.setUserId(userId);
            exchangeBalance.setAvailable(new BigDecimal(account.getAvailable()));
            exchangeBalance.setFreeze(new BigDecimal(account.getHold() == null ? "0" : account.getHold()));
            exchangeBalance.setLoan(new BigDecimal(account.getBorrowed() == null ? "0" : account.getBorrowed()));
            exchangeBalance.setFee(new BigDecimal(account.getFee() == null ? "0" : account.getFee()));
            exchangeBalance.setType(type);
            exchangeBalance.setuTime(new Date());
            //按照对照表加入coinId
            //存入coinId
            CoinContrasts coin = CoinContrastManager.okexContrasts.get(coinname.toUpperCase());
            if (coin == null) {
                log.debug("没有找到相应的币种对照表信息，币种 {}", coinname);
            } else {
                exchangeBalance.setCoin(coin.getCoin());
            }
            if (balanceListOld.size() > 0) {
                //获取数据与历史数据进行对比
                for (FirmOfferExchangeBalance obj : balanceListOld) {
                    if (Dic.WALLET.equalsIgnoreCase(obj.getType())) {
                        log.debug("钱包数据暂不加入获取订单行列,{}", obj);
                        continue;
                    }
                    if (obj.getAmount().compareTo(BigDecimal.ZERO)>0||exchangeBalance.getAmount().compareTo(BigDecimal.ZERO)>0){
                        changeSymbols.add(exchangeBalance.getSymbol());
                    }
                    //判断与历史数据相等---相等
                    if (obj.getCoin().equals(exchangeBalance.getCoin()) && obj.getSymbol().equals(exchangeBalance.getSymbol())) {
                        //判断总量变动情况---不相等或者其冻结资产有变化或者可用资产有变化或者借用资产有变化
                        log.debug("userId:{} type:{} symbol:{} oldAmount:{} newAmount:{} oldFreeze:{} newFreeze:{} oldVailable:{} newVailable:{} oldLoan:{} newLoan:{}  ", userId, type, obj.getSymbol(), obj.getAmount(), exchangeBalance.getAmount(), obj.getFreeze(), exchangeBalance.getFreeze()
                                , obj.getAvailable(), exchangeBalance.getAvailable(), obj.getLoan(), exchangeBalance.getLoan());
                        if (checkNeedOrders(exchangeBalance.getAmount(), obj.getAmount())
                                || checkNeedOrders(exchangeBalance.getAvailable(), obj.getAvailable())
                                || checkNeedOrders(exchangeBalance.getFreeze(), obj.getFreeze())
                                || checkNeedOrders(exchangeBalance.getLoan(), obj.getLoan())
                                || checkNeedOrders(exchangeBalance.getFee(), obj.getFee())) {
                            //余额变动 添加该币种到充值提现
                            changeSymbols.add(exchangeBalance.getSymbol());
                        }
                    }
                }
            } else {
                changeSymbols.add(exchangeBalance.getSymbol());
            }
            //如果这个币种没有币种id 库里数量不为0 并且  总量 可用 冻结 全部为0 就跳过不加入
            //&& !isBigdecimalZero(exchangeBalance,balanceOldMap)
            if (exchangeBalance.getCoin() != null) {
                balanceListNew.add(exchangeBalance);
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
            log.debug("userId {},type {},变动数据:{}", userId, type, JSON.toJSONString(balanceListNew));
            //批量更新
            replaceBlance(userId, balanceListNew, EX_CHANGE_OKEX, type.toUpperCase(), Dic.DELETE);
            if (changeSymbols.size() > 0 && !type.equalsIgnoreCase(WALLET)) {
                setOrderPending(userId, changeSymbols, type);
            }
        } else {
            //批量添加
            if (balanceListNew.size() < 1) {
                return;
            }
            replaceBlance(userId, balanceListNew, EX_CHANGE_OKEX, type.toUpperCase(), Dic.INSERT);
            //历史没有订单记录，现有余额，添加历史订单查询 活跃订单查询
            if (!type.equalsIgnoreCase(WALLET)) {
                setOrderPending(userId, changeSymbols, type);
            }
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


    boolean isBigdecimalZero(FirmOfferExchangeBalance exchangeBalance, Map<String, FirmOfferExchangeBalance> map) {
        if (map.get(exchangeBalance.getSymbol()) != null && map.get(exchangeBalance.getSymbol()).getAmount().compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }
        return exchangeBalance.getAvailable().compareTo(BigDecimal.ZERO) == 0
                && exchangeBalance.getFreeze().compareTo(BigDecimal.ZERO) == 0
                && exchangeBalance.getAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    private final String FUTURECROSSED = "crossed";
    private final String FUTUREFIXED = "fixed";

    /**
     * 调用接口获取余额-合约
     *
     * @param jsonObject
     * @return
     */
    private Map<String, Account> getbalancesFuture(Long userId, JSONObject jsonObject) {
        Map<String, Account> accounts = new HashMap<>(200);
        jsonObject.getJSONObject("info").entrySet().forEach(job -> {
            Account account = new Account();
            //Okex 期货分全仓和逐仓 这里需区分下
            JSONObject value = (JSONObject) job.getValue();
            String marginMode = value.getString("margin_mode");
            if (FUTURECROSSED.equalsIgnoreCase(marginMode)) {
                log.debug("userId {} 是全仓模式", userId);
                //全仓模式
                account.setCurrency(job.getKey());
                account.setAvailable(value.getString("total_avail_balance"));
                account.setBalance(value.getString("equity"));
                account.setHold(value.getString("margin"));
                accounts.put(job.getKey(), account);
                return;
            } else if (FUTUREFIXED.equalsIgnoreCase(marginMode)) {
                log.debug("userId {} 是逐仓仓模式", userId);
                account.setCurrency(job.getKey());
                account.setAvailable(value.getString("total_avail_balance"));
                account.setBalance(value.getString("equity"));
                account.setHold(value.getString("margin_frozen"));
                accounts.put(job.getKey(), account);
                return;
            }
            log.info("userId {} 是未知模式已跳过", userId);
        });
        return accounts;
    }

    private void setOrderPending(Long userId, List<String> symbol, String type) {
        log.debug("Okex需要获取订单的数据 userId {} type:{} symbol:{}", userId, type, symbol);
        PendingObj pendingObj = new PendingObj(getOfferKeys(userId), userId, symbol, type);
        synchronized (okexOrderHist) {
            okexOrderHist.add(pendingObj);
        }
        synchronized (okexActiveHist) {
            okexActiveHist.add(pendingObj);
        }
        synchronized (okexDepositHist) {
            okexDepositHist.add(pendingObj);
        }
        synchronized (okexTransferHist) {
            okexTransferHist.add(pendingObj);
        }
        synchronized (okexLedgerHist) {
            okexLedgerHist.add(pendingObj);
        }
    }

    /**
     * 现货的操作历史入库
     *
     * @param userId
     * @param orders
     */
    public void spotOrdersHis(Long userId, List<OrderInfo> orders) {
        List<FirmOfferMatchHist> list = new ArrayList<>(100);
        try {
            for (OrderInfo order : orders) {
                FirmOfferMatchHist hist = new FirmOfferMatchHist();
                hist.setId(order.getOrder_id());
                hist.setType(order.getSide() + "-" + order.getType());
                hist.setPrice(new BigDecimal(order.getPrice()));
                hist.setFieldAmount(new BigDecimal(order.getFilled_size()));
                //买入金额，市价买入时返回
                hist.setFieldPrice(new BigDecimal(order.getFilled_notional()));
                hist.setMatchDate(DateUtils.convertStringToDate(order.getTimestamp(), DateUtils.FULL_TIME_OKEX_FORMAT_UTC));
                hist.setMatchId(order.getOrder_id());
                hist.setFieldFees(BigDecimal.ZERO);
                //Okex 会区分方向，放入source中
                hist.setSource(order.getStatus());
                hist.setUserId(userId);
                hist.setExChange(EX_CHANGE_OKEX);
                hist.setSymbol(order.getInstrument_id());
                hist.setMatchType(STOCK);
                list.add(hist);
            }
            if (list.isEmpty()) {
                return;
            }
            //ON DUPLICATE KEY UPDATE 储存数据  储存数据 这里有个逻辑：如果库中这个id的已成交数量有变化才会进行下方的更新操作
            List<FirmOfferMatchHist> insertMatchs = getNeedUpdateMatchHists(list, userId);
            if (insertMatchs == null || insertMatchs.isEmpty()) {
                return;
            }
            replaceMatch(userId, insertMatchs, EX_CHANGE_OKEX, STOCK, REPLACE);
        } catch (Exception e) {
            log.error("huobi orders 数据转化异常 {} {} str {}", e, e.getMessage(), list);
        }
    }

    /**
     * 获取需要更新的现货订单信息
     * 1.首先查看内存中的老数据是否为 null或空集合，如果没有则认为是第一次，全部抓取
     * 2.将内存中的数据根据 orderId与新数据进行比较判断是否需要更新其订单信息
     * 3.返回需要更新的现货订单信息
     *
     * @param list
     * @param userId
     * @return
     */
    private List<FirmOfferMatchHist> getNeedUpdateMatchHists(List<FirmOfferMatchHist> list, Long userId) {
        List<FirmOfferMatchHist> oldMatchs = getMatchsMap(userId);
        if (oldMatchs.size() == 0) {
            //如果内存中的订单信息为 null，则返回全部数据
            return list;
        }
        Map<Long, FirmOfferMatchHist> oldMatshMap = oldMatchs.stream().collect(Collectors.toMap(FirmOfferMatchHist::getId, Function.identity()));
        return list.stream().filter(firmOfferMatchHist -> oldMatshMap.get(firmOfferMatchHist.getId()) == null
                || firmOfferMatchHist.getFieldAmount() == null
                || oldMatshMap.get(firmOfferMatchHist.getId()).getFieldAmount() == null
                || firmOfferMatchHist.getFieldAmount().compareTo(oldMatshMap.get(firmOfferMatchHist.getId()).getFieldAmount()) != 0).collect(Collectors.toList());
    }

    /**
     * 将期货操作历史入库
     *
     * @param userId
     * @param origin
     */
    public void futureOrderHist(Long userId, JSONObject origin) {
        boolean result = origin.getBooleanValue("result");
        if (!result) {
            log.warn("okex orderHist 未获取到正常数据 {}", origin);
            return;
        }
        try {
            List<JSONObject> orderslist = origin.getJSONArray("order_info").toJavaList(JSONObject.class);
            if (orderslist.isEmpty()) {
                log.debug("userId {} 未获取到订单信息,{}", userId, origin);
                return;
            }
            //存储数据
            List<FirmOfferOrderHist> list = new LinkedList<>();
            for (Map order : orderslist) {
                //amount: 挂单数量
                BigDecimal amount = new BigDecimal(order.get("size").toString());
                // contract_name: 合约名称
                String contractName = order.get("instrument_id").toString();
                // create_date: 委托时间
                Date createDate = DateUtils.coverStringFormatTime(order.get("timestamp").toString());
                // deal_amount: 成交数量
                BigDecimal dealAmount = new BigDecimal(order.get("filled_qty").toString());
                // fee: 手续费
                BigDecimal fee = new BigDecimal(order.get("fee").toString());
                // order_id: 订单ID
                String orderId = String.valueOf(order.get("order_id"));
                // price: 订单价格
                BigDecimal price = new BigDecimal(order.get("price").toString());
                // price_avg: 平均价格
                BigDecimal priceAvg = new BigDecimal(order.get("price_avg").toString());
                // status: 订单状态(-1.撤单成功；0:等待成交 1:部分成交 2:已完成）
                Integer orderStatus = Integer.valueOf(order.get("status").toString());
                // symbol: btc_usd ltc_usd eth_usd etc_usd bch_usd
                String symbol = (String) order.get("instrument_id");
                // type: 订单类型 1：开多 2：开空 3：平多 4： 平空
                String type = String.valueOf(order.get("type"));
                // unit_amount:合约面值
                BigDecimal unitAmount = new BigDecimal(order.get("contract_val").toString());
                // lever_rate: 杠杆倍数 value:10\20 默认10
                Integer leverRate = Integer.valueOf(order.get("leverage").toString());
                FirmOfferOrderHist hist = new FirmOfferOrderHist();
                hist.setId(orderId);
                hist.setOrderDate(createDate);
                hist.setTradingOn(symbol);
                hist.setType(type);
                hist.setPrice(price);
                hist.setSide("");
                hist.setAmount(amount);
                hist.setFieldAmount(dealAmount);
                hist.setFieldPrice(priceAvg);
                hist.setOrderStatus(orderStatus);
                hist.setUserId(userId);
                hist.setLeverRate(new BigDecimal(leverRate));
                hist.setUnitAmount(unitAmount);
                hist.setContractName(contractName);
                hist.setFee(fee);
                hist.setExChange(EX_CHANGE_OKEX);
                list.add(hist);
            }
            if (list.isEmpty()) {
                return;
            }
            //ON DUPLICATE KEY UPDATE 储存数据 这里有个逻辑：如果库中这个id的已成交数量有变化才会进行下方的更新操作
            List<FirmOfferOrderHist> insertOrders = getNeedUpdateOrderHists(list, userId);
            if (insertOrders == null || insertOrders.isEmpty()) {
                return;
            }
            replaceOrder(userId, insertOrders, EX_CHANGE_OKEX, FUTURE, REPLACE);
        } catch (Exception e) {
            log.error("okex orderHistApi 数据转化异常 str " + origin, e);
        }
    }

    public void swapOrdersHis(Long userId, JSONObject swapResults) {
        if (swapResults==null) {
            log.warn("okex orderHist 未获取到正常数据 {}", swapResults);
            return;
        }
        try {
            List<JSONObject> orderslist = swapResults.getJSONArray("order_info").toJavaList(JSONObject.class);
            if (orderslist.isEmpty()) {
                log.debug("userId {} 未获取到订单信息,{}", userId, swapResults);
                return;
            }
            //存储数据
            List<FirmOfferOrderHist> list = new LinkedList<>();
            for (Map order : orderslist) {
                //amount: 挂单数量
                BigDecimal amount = new BigDecimal(order.get("size").toString());
                // contract_name: 合约名称
                String contractName = order.get("instrument_id").toString();
                // create_date: 委托时间
                Date createDate = DateUtils.coverStringFormatTime(order.get("timestamp").toString());
                // deal_amount: 成交数量
                BigDecimal dealAmount = new BigDecimal(order.get("filled_qty").toString());
                // fee: 手续费
                BigDecimal fee = new BigDecimal(order.get("fee").toString());
                // order_id: 订单ID
                String orderId = String.valueOf(order.get("order_id"));
                // price: 订单价格
                BigDecimal price = new BigDecimal(order.get("price").toString());
                // price_avg: 平均价格
                BigDecimal priceAvg = new BigDecimal(order.get("price_avg").toString());
                // status: 订单状态(-2:失败 -1:撤销成功 0:等待成交 1:部分成交 2:完全成交)
                Integer orderStatus = Integer.valueOf(order.get("status").toString());
                // symbol: btc_usd ltc_usd eth_usd etc_usd bch_usd
                String symbol = order.get("instrument_id").toString().split("-")[0];
                // type: 订单类型 1：开多 2：开空 3：平多 4： 平空
                String type = String.valueOf(order.get("type"));
                // unit_amount:合约面值
                BigDecimal unitAmount = new BigDecimal(order.get("contract_val").toString());
                // lever_rate: 杠杆倍数 value:10\20 默认10
                Integer leverRate = 0;
                FirmOfferOrderHist hist = new FirmOfferOrderHist();
                hist.setId(orderId);
                hist.setOrderDate(createDate);
                hist.setTradingOn(contractName);
                hist.setType(type);
                hist.setPrice(price);
                hist.setSide("");
                hist.setAmount(amount);
                hist.setFieldAmount(dealAmount);
                hist.setFieldPrice(priceAvg);
                hist.setOrderStatus(orderStatus);
                hist.setUserId(userId);
                hist.setLeverRate(new BigDecimal(leverRate));
                hist.setUnitAmount(unitAmount);
                hist.setContractName(contractName);
                hist.setFee(fee);
                hist.setExChange(EX_CHANGE_OKEX);
                list.add(hist);
            }
            if (list.isEmpty()) {
                return;
            }
            //ON DUPLICATE KEY UPDATE 储存数据 这里有个逻辑：如果库中这个id的已成交数量有变化才会进行下方的更新操作
            List<FirmOfferOrderHist> insertOrders = getNeedUpdateOrderHists(list, userId);
            if (insertOrders == null || insertOrders.isEmpty()) {
                return;
            }
            replaceOrder(userId, insertOrders, EX_CHANGE_OKEX, SWAP, REPLACE);
        } catch (Exception e) {
            log.error("okex orderHistApi 数据转化异常 str " + swapResults, e);
        }
    }

    public void addSwapPosition(Long userId, JSONArray swapPositions) {
        if (swapPositions==null){
            log.warn("okex swap position userId {} 未获取到正常数据 {}",userId, swapPositions);
            return;
        }
        if (swapPositions.isEmpty()){
            return;
        }
        List<FirmOfferPosition> oldPositions = getPositionsMap(userId,SWAP);
        List<SwapPosition> swapPositionList = JSONArray.parseArray(JSON.toJSONString(swapPositions),SwapPosition.class);
        List<SwapPosition> collect = swapPositionList.stream().filter(swapPosition -> !swapPosition.getHolding().isEmpty()).collect(Collectors.toList());
        if (collect.isEmpty()){
            log.info("userId {} 未抓到存在的持仓,respose:{}", userId, collect);
            if (oldPositions.size() != 0) {
                Set<String> changeSymbol = oldPositions.stream().map(firmOfferPosition -> getSymbolTradingOn(firmOfferPosition)).collect(Collectors.toSet());
                setOrderPending(userId, new ArrayList<>(changeSymbol), SWAP);
            }
            replacePosition(userId, Collections.EMPTY_LIST, EX_CHANGE_OKEX, SWAP, DELETE);
            return;
        }
        collect.forEach(swapPosition -> {
            JSONArray holding = swapPosition.getHolding();
            List<FirmOfferPosition> positions = new ArrayList<>(10);
            holding.forEach(object -> {
                JSONObject jsonObject = (JSONObject) object;
                positions.add(getSwapPositionByType(userId, jsonObject));
            });
            Map<String, FirmOfferPosition> oldPositionMap = oldPositions.stream().collect(Collectors.toMap(firmOfferPosition -> firmOfferPosition.getId() + "_" + firmOfferPosition.getExChange(), Function.identity(), (o1, o2) -> o1));
            Map<String, FirmOfferPosition> newPositionMap = positions.stream().collect(Collectors.toMap(firmOfferPosition -> firmOfferPosition.getId() + "_" + firmOfferPosition.getExChange(), Function.identity(), (o1, o2) -> o1));
            Set<String> changeSymbol = positions.stream().filter(firmOfferPosition -> getNewAndOldExist(oldPositionMap, firmOfferPosition)).map(firmOfferPosition -> getSymbolTradingOn(firmOfferPosition)).collect(Collectors.toSet());
            changeSymbol.addAll(oldPositions.stream().filter(firmOfferPosition -> getNewAndOldExist(newPositionMap, firmOfferPosition)).map(firmOfferPosition -> getSymbolTradingOn(firmOfferPosition)).collect(Collectors.toSet()));
            setOrderPending(userId, new ArrayList<>(changeSymbol), SWAP);
            replacePosition(userId, positions, EX_CHANGE_OKEX, SWAP, DELETE);
        });
    }

    private String getSymbolTradingOn(FirmOfferPosition firmOfferPosition) {
        String tradingOn = firmOfferPosition.getTradingOn();
        String[] split = tradingOn.split("-");
        return split[0].toUpperCase();
    }

    @Data
    static class SwapPosition{
        private String margin_mode;
        private JSONArray holding;
    }

    public void addFuturePosition(Long userId, JSONObject futurePosition) {
        boolean result = futurePosition.getBooleanValue("result");
        if (!result) {
            log.warn("okex orderHist userId {} 未获取到正常数据 {}",userId, futurePosition);
            return;
        }
        List<FirmOfferPosition> oldPositions = getPositionsMap(userId, FUTURE);
        //仓位区分全仓逐仓
        JSONArray holding = futurePosition.getJSONArray("holding");
        if (holding == null || holding.isEmpty()) {
            log.info("userId {} 未抓到存在的持仓,respose:{}", userId, futurePosition);
            if (oldPositions.size() != 0) {
                Set<String> changeSymbol = oldPositions.stream().map(firmOfferPosition -> {
                    String tradingOn = firmOfferPosition.getTradingOn();
                    String[] split = tradingOn.split("-");
                    return split[0];
                }).collect(Collectors.toSet());
                setOrderPending(userId, new ArrayList<>(changeSymbol), FUTURE);
            }
            replacePosition(userId, Collections.EMPTY_LIST, EX_CHANGE_OKEX, FUTURE, DELETE);
            return;
        }
        List<FirmOfferPosition> positions = new ArrayList<>(10);
        holding.forEach(array -> {
            JSONArray jsonArray = (JSONArray) array;
            jsonArray.forEach(obj -> {
                JSONObject jsonObject = (JSONObject) obj;
                positions.add(getPositionByType(userId, jsonObject, "long"));
                positions.add(getPositionByType(userId, jsonObject, "short"));
            });
        });
        Map<String, FirmOfferPosition> oldPositionMap = oldPositions.stream().collect(Collectors.toMap(firmOfferPosition -> firmOfferPosition.getId() + "_" + firmOfferPosition.getExChange(), Function.identity(), (o1, o2) -> o1));
        Map<String, FirmOfferPosition> newPositionMap = positions.stream().collect(Collectors.toMap(firmOfferPosition -> firmOfferPosition.getId() + "_" + firmOfferPosition.getExChange(), Function.identity(), (o1, o2) -> o1));
        Set<String> changeSymbol = positions.stream().filter(firmOfferPosition -> getNewAndOldExist(oldPositionMap, firmOfferPosition)).map(firmOfferPosition -> getSymbolTradingOn(firmOfferPosition)).collect(Collectors.toSet());
        changeSymbol.addAll(oldPositions.stream().filter(firmOfferPosition -> getNewAndOldExist(newPositionMap, firmOfferPosition)).map(firmOfferPosition -> getSymbolTradingOn(firmOfferPosition)).collect(Collectors.toSet()));
        setOrderPending(userId, new ArrayList<>(changeSymbol), FUTURE);
        replacePosition(userId, positions, EX_CHANGE_OKEX, FUTURE, DELETE);
    }

    private boolean getNewAndOldExist(Map<String, FirmOfferPosition> newPositionMap, FirmOfferPosition firmOfferPosition) {
        FirmOfferPosition newFirmOfferPossition = newPositionMap.get(firmOfferPosition.getId() + "_" + firmOfferPosition.getExChange());
        if (null == newFirmOfferPossition) {
            //老的有  新的没有
            return true;
        }
        return firmOfferPosition.getQty().compareTo(newFirmOfferPossition.getQty()) != 0;
    }

    /**
     * 根据多空类型获取相应持仓
     *
     * @param userId
     * @param jsonObject
     */
    private FirmOfferPosition getSwapPositionByType(Long userId, JSONObject jsonObject) {
        FirmOfferPosition position = new FirmOfferPosition();
        String margin_mode = jsonObject.getString("margin_mode");
        position.setUserId(userId);
        position.setExChange(EX_CHANGE_OKEX);
        position.setMarginMode(margin_mode);
        String instrument_id = jsonObject.getString("instrument_id");
        position.setTradingOn(instrument_id);
        String type = jsonObject.getString("side");
        String symbol = instrument_id.split("-")[0];
        position.setType(type);
        position.setInstrumentId(symbol+"永续合约");
        position.setPositionDate(DateUtils.coverStringFormatTime(jsonObject.getString("timestamp")));
        position.setUtime(new Date());
        position.setAvailQty(jsonObject.getBigDecimal("avail_position"));
        position.setAvgCost(jsonObject.getBigDecimal("avg_cost"));
        position.setQty(jsonObject.getBigDecimal("position"));
        position.setRealizedPnl(jsonObject.getBigDecimal("realised_pnl"));
        position.setLeverage(jsonObject.getString("leverage"));
        position.setLiquidationPrice(jsonObject.getBigDecimal("liquidation_price"));
        position.setSettlementPrice(jsonObject.getBigDecimal("settlement_price"));
        position.setLiquiPrice(jsonObject.getBigDecimal("liquidation_price"));
        return position;
    }

    /**
     * 根据多空类型获取相应持仓
     *
     * @param userId
     * @param jsonObject
     * @param type       long/short
     */
    private FirmOfferPosition getPositionByType(Long userId, JSONObject jsonObject, String type) {
        FirmOfferPosition position = new FirmOfferPosition();
        String margin_mode = jsonObject.getString("margin_mode");
        position.setUserId(userId);
        position.setExChange(EX_CHANGE_OKEX);
        position.setMarginMode(margin_mode);
        String instrument_id = jsonObject.getString("instrument_id");
        position.setTradingOn(instrument_id);
        position.setInstrumentId(getOwnNameByInstrumentId(instrument_id, type));
        position.setPositionDate(DateUtils.coverStringFormatTime(jsonObject.getString("created_at")));
        position.setUtime(DateUtils.coverStringFormatTime(jsonObject.getString("updated_at")));
        position.setAvailQty(jsonObject.getBigDecimal(type + "_avail_qty"));
        position.setAvgCost(jsonObject.getBigDecimal(type + "_avg_cost"));
        position.setQty(jsonObject.getBigDecimal(type + "_qty"));
        position.setType(type);
        position.setRealizedPnl(jsonObject.getBigDecimal("realised_pnl"));
        //全仓模式
        if (margin_mode.equalsIgnoreCase(AllSTORE)) {
            position.setLeverage(jsonObject.getString("leverage"));
            position.setLiquidationPrice(jsonObject.getBigDecimal("liquidation_price"));
        }
        //逐仓模式
        if (margin_mode.equalsIgnoreCase(LIMITSTORE)) {
            position.setSettlementPrice(jsonObject.getBigDecimal(type + "_settlement_price"));
            position.setPnlRatio(jsonObject.getBigDecimal(type + "_pnl_ratio").multiply(new BigDecimal(100).setScale(4, BigDecimal.ROUND_HALF_DOWN)));
            position.setLeverage(jsonObject.getString(type + "_leverage"));
            position.setLiquiPrice(jsonObject.getBigDecimal(type + "_liqui_price"));
        }
        return position;
    }

    /**
     * 根据Okex 返回InstrumentId 返回不同我们的定义名 例如XRP-USD-181228 换算为XRP季度多
     *
     * @param instrumentId
     * @return
     */
    private String getOwnNameByInstrumentId(String instrumentId, String type) {
        StringBuilder stringBuilder = new StringBuilder();
        String[] instrumentIds = instrumentId.split("-");
        if (instrumentIds == null || instrumentIds.length != 3) {
            return instrumentId;
        }
        String time = instrumentIds[2];
        String contract = contractUtil.judgeContract(time);
        String key;
        switch (contract) {
            case "当周合约":
                key = "当周";
                break;
            case "次周合约":
                key = "次周";
                break;
            case "季度合约":
                key = "季度";
                break;
            default:
                key = "";
        }
        stringBuilder.append(instrumentIds[0]).append(key);
        if (Dic.LONG.equalsIgnoreCase(type)) {
            stringBuilder.append("多");
        }
        if (Dic.SHORT.equalsIgnoreCase(type)) {
            stringBuilder.append("空");
        }
        return stringBuilder.toString();
    }

    public void addDepositHistory(Long userId, JSONArray depositHistory) {
        if (depositHistory == null || depositHistory.isEmpty()) {
            log.warn("userId {} deposit is Empty，continue", userId);
            return;
        }
        List<FirmOfferDepositHist> firmOfferDepositHists = depositHistory.stream().map(deposit -> {
            FirmOfferDepositHist firmOfferDepositHist = JSON.toJavaObject(JSONObject.parseObject(deposit.toString()), FirmOfferDepositHist.class);
            return firmOfferDepositHist;
        }).collect(Collectors.toList());
        replaceDeposit(userId, firmOfferDepositHists, EX_CHANGE_OKEX, FUTURE, REPLACE);
    }

    public void addLedgerHistory(Long userId, List<Ledger> ledgers, String type) {
        if (ledgers == null || ledgers.isEmpty()) {
            log.warn("userId {} ledgers is Empty，continue", userId);
            return;
        }
        List<FirmOfferLedgerHist> firmOfferLedgerHists = ledgers.stream().map(ledger -> {
            FirmOfferLedgerHist firmOfferLedgerHist = new FirmOfferLedgerHist();
            firmOfferLedgerHist.setAmount(ledger.getAmount());
            firmOfferLedgerHist.setBalance(ledger.getBalance());
            firmOfferLedgerHist.setCurrency(ledger.getCurrency());
            firmOfferLedgerHist.setExchange(EX_CHANGE_OKEX);
            firmOfferLedgerHist.setFee(ledger.getFee());
            firmOfferLedgerHist.setUserId(userId);
            firmOfferLedgerHist.setLedgerId(ledger.getLedger_id().toString());
            firmOfferLedgerHist.setTypeName(ledger.getTypeName());
            firmOfferLedgerHist.setType(ledger.getType());
            firmOfferLedgerHist.setTimestamp(DateUtils.coverStringFormatTime(ledger.getTimestamp()));
            return firmOfferLedgerHist;
        }).collect(Collectors.toList());
        replaceLedger(userId, firmOfferLedgerHists, EX_CHANGE_OKEX, type, REPLACE);
    }

    /**
     * 杠杆交易订单
     *
     * @param userId
     * @param marginResults
     */
    public void marginOrdersHis(Long userId, List<OrderInfo> marginResults) {
        List<FirmOfferMatchHist> list = new ArrayList<>(100);
        try {
            for (OrderInfo order : marginResults) {
                FirmOfferMatchHist hist = new FirmOfferMatchHist();
                hist.setId(order.getOrder_id());
                hist.setType(order.getSide() + "-" + order.getType());
                BigDecimal filledSize = new BigDecimal(order.getFilled_size());
                if (filledSize.compareTo(BigDecimal.ZERO) == 0) {
                    //这种情况只会在撤单时出现，除1保证不会除零切原数据不变
                    filledSize = BigDecimal.ONE;
                }
                if ("market".equals(order.getType())) {
                    hist.setPrice(new BigDecimal(order.getFilled_notional())
                            .divide(filledSize, 8, BigDecimal.ROUND_HALF_DOWN));
                } else if ("limit".equals(order.getType())) {
                    hist.setPrice(new BigDecimal(order.getPrice()));
                } else {
                    log.warn("杠杆订单数据有问题，请检查文档,order:{}", JSON.toJSONString(order));
                    continue;
                }
                //买入金额，市价买入时返回
                hist.setFieldPrice(new BigDecimal(order.getFilled_notional())
                        .divide(filledSize, 8, BigDecimal.ROUND_HALF_DOWN));
                hist.setFieldAmount(new BigDecimal(order.getFilled_size()));
                hist.setMatchDate(DateUtils.coverStringFormatTime(order.getTimestamp()));
                hist.setMatchId(order.getOrder_id());
                hist.setFieldFees(BigDecimal.ZERO);
                //Okex 会区分方向，放入source中
                hist.setSource(order.getStatus());
                hist.setUserId(userId);
                hist.setExChange(EX_CHANGE_OKEX);
                hist.setSymbol(order.getInstrument_id());
                hist.setMatchType(MARGIN);
                list.add(hist);
            }
            if (list.isEmpty()) {
                return;
            }
            //ON DUPLICATE KEY UPDATE 储存数据  储存数据 这里有个逻辑：如果库中这个id的已成交数量有变化才会进行下方的更新操作
            List<FirmOfferMatchHist> insertMatchs = getNeedUpdateMatchHists(list, userId);
            if (insertMatchs == null || insertMatchs.isEmpty()) {
                return;
            }
            replaceMatch(userId, insertMatchs, EX_CHANGE_OKEX, MARGIN, REPLACE);
        } catch (Exception e) {
            log.error("杠杆交易出错", e);
            log.error("okex margin orders 数据转化异常 str {}", list);
        }
    }

}
