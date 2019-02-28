package com.ailu.firmoffer.task;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.config.ExchangeName;
import com.ailu.firmoffer.dao.bean.FirmOfferKey;
import com.ailu.firmoffer.dao.bean.FirmOfferKeyExample;
import com.ailu.firmoffer.dao.mapper.FirmOfferKeyMapper;
import com.ailu.firmoffer.dao.mapper.ext.FirmOfferExchangeBalanceExt;
import com.ailu.firmoffer.dao.mapper.ext.FirmOfferKeyExt;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.exchange.apiclient.OkexV3ApiClient;
import com.ailu.firmoffer.exchange.conversion.OkexV3Conversion;
import com.ailu.firmoffer.util.DateUtils;
import com.ailu.firmoffer.vo.UserBalanceKeys;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okcoin.okex.open.api.bean.account.result.Ledger;
import com.okcoin.okex.open.api.bean.account.result.Wallet;
import com.okcoin.okex.open.api.bean.spot.result.Account;
import com.okcoin.okex.open.api.bean.spot.result.OrderInfo;
import com.okcoin.okex.open.api.bean.spot.result.UserMarginBillDto;
import com.okcoin.okex.open.api.exception.APIException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.ailu.firmoffer.task.Pending.*;

/**
 * @Description: Okex 的V3 api 接口调用
 * @author: mr.wang
 * @version: V1.0
 * @date: 2018年11月9日15:20:17
 */
@Slf4j
@Component
public class OkexV3Task implements ExChangeTask {

    @Resource
    OkexV3Conversion okexV3Conversion;

    private static String symbol = "_btc,_usdt,_eth,_bch";

    List<String> contractTypes = Arrays.asList("this_week", "next_week", "quarter");

    List<String> units = Arrays.asList("USDT", "ETH", "BTC", "OKB");

    @Value("${spring.proxy.enable}")
    public boolean proxyEnable;

    @Value("${spring.proxy.url}")
    public String url;
    @Value("${spring.proxy.port}")
    public String port;

    @Value("${spider.num}")
    public String spiderNum;

    @Resource
    private FirmOfferKeyExt firmOfferKeyExt;

    @Autowired
    private FirmOfferExchangeBalanceExt firmOfferExchangeBalanceExt;

    final List<String> MARGINUNIT = Arrays.asList("BTC", "USDT");

    private static final Set<String> okexMarginSymbol = new HashSet<>();

    public Set<String> getOkexMarginSymbol() {
        return okexMarginSymbol;
    }

    public static void addOkexMarginSymbol(List<String> symbols) {
        okexMarginSymbol.addAll(symbols);
    }

    /**
     *
     * 定时条件：5分钟内不能超过3000次，所以每个任务间隔时间必须大于100ms。
     */

    /**
     * 获取合约和币币账户余额
     * 访问频率 6次/2秒（币币），合约默认
     */
    @Override
    public void balances() {
        Map<Long, FirmOfferKey> offerKeys = new HashMap<>(okexV3Conversion.getOfferKeys());
        offerKeys.forEach((userId, secret) -> {
            OkexV3ApiClient client = new OkexV3ApiClient(secret.getApikey(), secret.getApikeysecret(), secret.getPassphrase(), proxyEnable, url, port);
            JSONObject futureResults = null;
            List<Account> stockResults = null;
            List<Wallet> walletResults = null;
            JSONObject swapResult = null;
            List<Map<String, Object>> marginResult = null;
            try {
                getAllBlance(userId, client);
            } catch (Throwable e) {
                if (e.getMessage().contains("30012")||e.getMessage().contains("30006")){
                    log.error("Okex user key Invalid Authority,userId : {}",userId);
                    firmOfferKeyExt.updateUserSpiderNum("88",userId);
                }
                log.error("获取Okex期货账户余额出错！用户id " + userId, e);
            }
        });
    }

    /**
     * 五个必须同时完整抓取才会触发推送 错误一个都会触发重新抓取
     * @param userId
     * @param client
     * @throws InterruptedException
     */
    private void getAllBlance(Long userId, OkexV3ApiClient client) throws InterruptedException {
        JSONObject futureResults;
        List<Account> stockResults;
        List<Wallet> walletResults;
        JSONObject swapResult;
        List<Map<String, Object>> marginResult;
        futureResults = client.getFutureAccounts();
        log.debug("OkexTask ：user id balances : {} , futureResults{}", userId, futureResults);
        Thread.sleep(2000L);
        stockResults = client.getSpotAccounts();
        log.debug("OkexTask ：user id balances : {}, stockResults{} ", userId, stockResults);
        Thread.sleep(2000L);
        walletResults = client.getWallet();
        log.debug("OkexTask ：user id balances : {} , walletResults{}", userId, walletResults);
        Thread.sleep(2000L);
        swapResult = client.getSwapAccount();
        log.debug("OkexTask ：user id balances : {} , swapResult{}", userId, swapResult);
        Thread.sleep(2000L);
        marginResult = client.getMarginAccount();
        log.debug("OkexTask ：user id balances : {} , marginResult{}", userId, marginResult);
        Thread.sleep(2000L);
        okexV3Conversion.addFutureBalances(userId, futureResults);
        okexV3Conversion.addStockBalances(userId, stockResults);
        okexV3Conversion.addWalletBalances(userId, walletResults);
        okexV3Conversion.addSwapBalances(userId, swapResult);
        okexV3Conversion.addMarginBalances(userId, marginResult);
    }

    /**
     * 获取 Okex 持仓
     */
    @Override
    public void position() {
//        Map<Long, FirmOfferKey> offerKeys = new HashMap<>(okexV3Conversion.getOfferKeys());
//        offerKeys.forEach((userId, secret) -> {
//            OkexV3ApiClient client = new OkexV3ApiClient(secret.getApikey(), secret.getApikeysecret(), secret.getPassphrase(), proxyEnable, url, port);
//            JSONObject futurePosition = null;
//            try {
//                futurePosition = client.getFuturePositions();
//                okexV3Conversion.addFuturePosition(userId, futurePosition);
//                Thread.sleep(2000L);
//                JSONArray swapPositions = client.getSwapPositions();
//                okexV3Conversion.addSwapPosition(userId, swapPositions);
//                Thread.sleep(2000L);
//            } catch (Throwable e) {
//                log.error("获取Okex持仓信息出错！用户id " + userId, e);
//            }
//        });
    }

    private List<Integer> swapStatus = Arrays.asList(6,7);

    /**
     * 调用频率限制，每分钟一次o
     */
    @Override
    public void ordersHist() {
        if (okexOrderHist.size() > 0) {
            List<PendingObj> list = new ArrayList<>(okexOrderHist);
            log.trace("订单list:{}", list);
            list.forEach(pendingObj -> {
                OkexV3ApiClient client = new OkexV3ApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret(), pendingObj.getKey().getPassphrase(), proxyEnable, url, port);
                List<OrderInfo> spotResults = null;
                JSONObject futureResults;
                List<OrderInfo> marginResults = null;
                JSONObject swapResults = null;
                if (pendingObj.getSymbols() == null || pendingObj.getSymbols().isEmpty()) {
                    return;
                }
                for (String symbol : pendingObj.getSymbols()) {
                    if (StringUtils.isEmpty(symbol)) {
                        subtractOne(okexOrderHist, pendingObj);
                        continue;
                    }
                    try {
                        if (Objects.equals(pendingObj.getType(), Dic.STOCK) && !symbol.equalsIgnoreCase("usdt")) {
                            log.info("开始查询用户{}的{}币种 - 现货", pendingObj.getUserId(), symbol);
                            for (String unit : units) {
                                try {
                                    spotResults = client.getSpotOrders(symbol + "-" + unit);
                                    okexV3Conversion.spotOrdersHis(pendingObj.getUserId(), spotResults);
                                    Thread.sleep(1000L);
                                    log.debug("OkexTask:spot user id ordersHist: {} , {} ", pendingObj.getUserId(), spotResults);
                                } catch (APIException e) {
                                    continue;
                                }
                            }
                        } else if (Objects.equals(pendingObj.getType(), Dic.FUTURE)) {
                            for (String contractType : contractTypes) {
                                switch (contractType) {
                                    case "this_week":
                                        getFutureordersByDate(pendingObj, client, symbol, contractType, DateUtils.getFridayForThisWeek());
                                        break;
                                    case "next_week":
                                        getFutureordersByDate(pendingObj, client, symbol, contractType, DateUtils.getFridayForNextWeek());
                                        break;
                                    case "quarter":
                                        getFutureordersByDate(pendingObj, client, symbol, contractType, DateUtils.getFridayForThisQuarterByOkex());
                                        break;
                                    default:
                                        break;
                                }
                                Thread.sleep(1000L);
                            }
                        } else if (Objects.equals(pendingObj.getType(), Dic.MARGIN)) {
                            log.info("开始查询用户{}的{}币种 - 杠杆", pendingObj.getUserId(), symbol);
                            for (String unit : units) {
                                try {
                                    marginResults = client.getMarginOrders(symbol + "-" + unit, "all", null, null, null);
                                    okexV3Conversion.marginOrdersHis(pendingObj.getUserId(), marginResults);
                                    Thread.sleep(1000L);
                                    log.debug("OkexTask:margin user id ordersHist: {} , {} ", pendingObj.getUserId(), marginResults);
                                } catch (APIException e) {
                                    continue;
                                }
                            }
                        }else if (Objects.equals(pendingObj.getType(), Dic.SWAP)) {
                            log.info("开始查询用户{}的{}币种 - 永续", pendingObj.getUserId(), symbol);
                            for (Integer status:swapStatus) {
                                try {
                                    swapResults = client.getSwapOrders(symbol +"-USD-SWAP", status);
                                    okexV3Conversion.swapOrdersHis(pendingObj.getUserId(), swapResults);
                                    Thread.sleep(1000L);
                                    log.debug("OkexTask:swap user id ordersHist: {} , {} ", pendingObj.getUserId(), marginResults);
                                } catch (APIException e) {
                                    continue;
                                }
                            }
                        }
                        Thread.sleep(3000);
                    } catch (Throwable e) {
                        log.error("获取 Okex 订单出错 用户id " + pendingObj.getUserId(), e);
                        continue;
                    }
                    subtractOne(okexOrderHist, pendingObj);
                }
            });
            //TODO 2018年11月28日21:05:46 改为一个一个移除，如果成功就移除
            //refresh(okexOrderHist);
        }
    }

    @Override
    public void matchsHist() {

    }

    private void getFutureordersByDate(PendingObj pendingObj, OkexV3ApiClient client, String symbol, String contractType, Date fridayForThisQuarter) {
        JSONObject futureResults;
        String time = DateUtils.convertDateToString(fridayForThisQuarter, DateUtils.DATE_SIMPLE_SHORT_FORMAT);
        log.info("开始查询用户{}的{}币种{}合约历史,时间{}", pendingObj.getUserId(), symbol, contractType, time);
        futureResults = client.getFutureOrders(symbol.toUpperCase() + "-USD-" + time, 6,pendingObj.getUserId());
        okexV3Conversion.futureOrderHist(pendingObj.getUserId(), futureResults);
        log.debug("OkexTask:future user id ordersHist:{} , {} ", pendingObj.getUserId(), futureResults);
        futureResults = client.getFutureOrders(symbol.toUpperCase() + "-USD-" + time, 7,pendingObj.getUserId());
        okexV3Conversion.futureOrderHist(pendingObj.getUserId(), futureResults);
        log.debug("OkexTask:future user id ordersHist:{} , {} ", pendingObj.getUserId(), futureResults);
    }

    /**
     * Okex 充值历史
     */
    //@Scheduled(initialDelay = 10 * 1000, fixedDelay = 100)
    public void depositHistory() {
        if (okexDepositHist.size() > 0) {
            List<PendingObj> list = new ArrayList<>(okexDepositHist);
            log.trace("deposit request list:{}", list);
            list.forEach(pendingObj -> {
                try {
                    OkexV3ApiClient client = new OkexV3ApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret(), pendingObj.getKey().getPassphrase(), proxyEnable, url, port);
                    JSONArray depositHistory = null;
                    depositHistory = client.getDepositHistory();
                    okexV3Conversion.addDepositHistory(pendingObj.getUserId(), depositHistory);
                    log.debug("OkexTask:user id :{} ,deposit: {} ", pendingObj.getUserId(), depositHistory);
                } catch (Throwable e) {
                    log.error("get userId " + pendingObj.getUserId() + " deposit error", e);
                    return;
                }
                subtractOne(okexDepositHist, pendingObj);
            });
        }
    }

    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 1000)
    public void ledgerHistory() {
        log.debug("started okex kedger history ");
        if (okexLedgerHist.isEmpty()||okexNewOrderHist.isEmpty()) {
            return;
        }
        List<PendingObj> list = new ArrayList<>(okexLedgerHist);
        list.addAll(okexNewOrderHist);
        log.trace("ledger request list:{}", list);
        list.forEach(pendingObj -> {
            OkexV3ApiClient client = null;
            Map<String,List<Ledger>> map;
            try {
                client = new OkexV3ApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret(), pendingObj.getKey().getPassphrase(), proxyEnable, url, port);
                if (pendingObj.getType() == null) {
                    log.warn("pendingObj.getType() is null ,continues. pendingObj : {}", pendingObj);
                    return;
                }
                map = getAllLedger(pendingObj, client);
            } catch (Throwable e) {
                log.error("get userId " + pendingObj.getUserId() + " ledger error", e);
                try {
                    map = getAllLedger(pendingObj,client);
                }catch (Throwable repeatE){
                    log.error("repeat get userId " + pendingObj.getUserId() + " ledger error", repeatE);
                    return;
                }
                return;
            }
            okexV3Conversion.addLedgerHistory(pendingObj.getUserId(), map.get(Dic.STOCK), Dic.STOCK);
            okexV3Conversion.addLedgerHistory(pendingObj.getUserId(), map.get(Dic.FUTURE), Dic.FUTURE);
            okexV3Conversion.addLedgerHistory(pendingObj.getUserId(), map.get(Dic.MARGIN), Dic.MARGIN);
            okexV3Conversion.addLedgerHistory(pendingObj.getUserId(), map.get(Dic.SWAP), Dic.SWAP);
            okexV3Conversion.addLedgerHistory(pendingObj.getUserId(), map.get(Dic.WALLET), Dic.WALLET);
            subtractOne(okexLedgerHist, pendingObj);
        });
        log.debug("ended okex kedger history ");
    }

    private Map<String,List<Ledger>> getAllLedger(PendingObj pendingObj, OkexV3ApiClient client) {
        Map<String,List<Ledger>> map = new HashMap<>();
        List<Ledger> wallteLedgers;
        List<Ledger> spotLedgers;
        List<Ledger> futureLedgers;
        List<Ledger> marginLedgers;
        List<Ledger> swapLedgers;
        wallteLedgers = client.getWalletLedger();
        map.put(Dic.WALLET,wallteLedgers);
        log.debug("OkexTask:user id :{} ,ledgers: {} ", pendingObj.getUserId(), wallteLedgers);
        switch (pendingObj.getType()) {
            case Dic.STOCK:
                log.info("开始抓取userId{} symbols{}的ledger,币币",pendingObj.getUserId(),pendingObj.getSymbols());
                spotLedgers = getSpotLedgers(client, pendingObj.getSymbols());
                map.put(Dic.STOCK,spotLedgers);
                log.debug("OkexTask:user id :{} ,spot ledgers: {} ", pendingObj.getUserId(), spotLedgers);
                break;
            case Dic.FUTURE:
                log.info("开始抓取userId{} symbols{}的ledger,交割合约",pendingObj.getUserId(),pendingObj.getSymbols());
                futureLedgers = getFutureLedgers(client, pendingObj.getSymbols());
                map.put(Dic.FUTURE,futureLedgers);
                log.debug("OkexTask:user id :{} ,future ledgers: {} ", pendingObj.getUserId(), futureLedgers);
                break;
            case Dic.MARGIN:
                log.info("开始抓取userId{} symbols{}的ledger,杠杆",pendingObj.getUserId(),pendingObj.getSymbols());
                marginLedgers = getMarginLedgers(client, pendingObj.getSymbols());
                map.put(Dic.MARGIN,marginLedgers);
                log.debug("OkexTask:user id :{} ,margin ledgers: {} ", pendingObj.getUserId(), marginLedgers);
                break;
            case Dic.SWAP:
                log.info("开始抓取userId{} symbols{}的ledger,永续合约",pendingObj.getUserId(),pendingObj.getSymbols());
                swapLedgers = getSwapLedgers(client, pendingObj.getSymbols());
                map.put(Dic.SWAP,swapLedgers);
                log.debug("OkexTask:user id :{} ,swap ledgers: {} ", pendingObj.getUserId(), swapLedgers);
                break;
            default:
                log.warn("Other ledger type ,continues type : {}", pendingObj.getType());
                break;
        }
        return map;
    }

    /**
     * okex 单独查询 期货订单程序所需Set
     **/
    public static Set<PendingObj> okexNewOrderHist = Collections.synchronizedSet(new HashSet<>());

    /**
     * Okex 独有的 期货订单增强定时
     * 2019年1月11日14:09:29
     * 需求 提出人：刘建书
     * 1.建一个表，存每个大V期货钱包各个币种的数量；（低频爬取）
     * 2.对每个大V超过阈值的币种，定时获取ORDER信息（高频）
     * 实际逻辑：
     * 由于需求是要求新建一个项目做这个但最终同意是在实盘蜘蛛里增加这个方法
     * 1.直接查库中期货资产大于0的币种和用户id
     * 2.根据这些信息去分线程查询订单数据
     * 2019年1月24日14:55:08
     * 从抓取资产币种数量变为抓取持仓数量
     * 1.抓取本次抓取多空与上次抓取对比，如果发生数量变化，则触发抓取合约订单；
     * 2.上次没，但这次有，或上次有，这次没也属数量变化
     */
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 1000)
    public void orderHistoryTask() {
        if (okexNewOrderHist.isEmpty()) {
            return;
        }
        List<PendingObj> list = new ArrayList<>(okexNewOrderHist);
        log.trace("okex new orders request list:{}", list);
        list.forEach(pendingObj -> {
            OkexV3ApiClient client = new OkexV3ApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret(), pendingObj.getKey().getPassphrase(), proxyEnable, url, port);
            if (pendingObj.getSymbols() == null || pendingObj.getSymbols().isEmpty()) {
                return;
            }
            for (String symbol : pendingObj.getSymbols()) {
                if (StringUtils.isEmpty(symbol)) {
                    subtractOne(okexOrderHist, pendingObj);
                    continue;
                }
                try {
                    if (Objects.equals(pendingObj.getType(), Dic.FUTURE)) {
                        for (String contractType : contractTypes) {
                            switch (contractType) {
                                case "this_week":
                                    getFutureordersByDate(pendingObj, client, symbol, contractType, DateUtils.getFridayForThisWeek());
                                    break;
                                case "next_week":
                                    getFutureordersByDate(pendingObj, client, symbol, contractType, DateUtils.getFridayForNextWeek());
                                    break;
                                case "quarter":
                                    getFutureordersByDate(pendingObj, client, symbol, contractType, DateUtils.getFridayForThisQuarterByOkex());
                                    break;
                                default:
                                    break;
                            }
                        }
                        Thread.sleep(2000);
                        subtractOne(okexOrderHist, pendingObj);
                    }else if (Objects.equals(pendingObj.getType(), Dic.SWAP)) {
                        log.info("开始查询用户{}的{}币种 - 永续", pendingObj.getUserId(), symbol);
                        for (Integer status:swapStatus) {
                            try {
                                JSONObject swapResults = client.getSwapOrders(symbol +"-USD-SWAP", status);
                                okexV3Conversion.swapOrdersHis(pendingObj.getUserId(), swapResults);
                                Thread.sleep(1000L);
                                log.debug("OkexTask:swap user id ordersHist: {} , {} ", pendingObj.getUserId(), swapResults);
                            } catch (APIException e) {
                                continue;
                            }
                        }
                        subtractOne(okexOrderHist, pendingObj);
                    }
                } catch (Throwable e) {
                    log.error("获取 Okex 订单出错 用户id " + pendingObj.getUserId(), e);
                    continue;
                }
            }
        });
        log.info("new future order history task ended");
    }

    @Scheduled(initialDelay = 100, fixedDelay = 10 * 60 * 1000)
    private void getAllUserFutureBalance() {
        log.info("okex get future orders started");
        List<UserBalanceKeys> firmOfferExchangeBalances = firmOfferExchangeBalanceExt.getBalanceByFuture(spiderNum);
        if (firmOfferExchangeBalances == null || firmOfferExchangeBalances.isEmpty()) {
            log.warn("本次获取Okex独立获取期货订单无用户满足条件，已跳过");
            return;
        }
        Map<String, List<UserBalanceKeys>> collect = firmOfferExchangeBalances.stream()
                .collect(Collectors.groupingBy(UserBalanceKeys::getKey, Collectors.toList()));
        okexNewOrderHist.addAll(collect.entrySet().stream()
                .filter(map -> map.getValue() != null && !map.getValue().isEmpty()).map(map -> {
                    FirmOfferKey key = new FirmOfferKey();
                    PendingObj pendingObj = new PendingObj();
                    key.setApikey(map.getValue().get(0).getApiKey());
                    key.setApikeysecret(map.getValue().get(0).getApiKeySecret());
                    key.setPassphrase(map.getValue().get(0).getPassphrase());
                    pendingObj.setKey(key);
                    String[] keys = map.getKey().split("_");
                    pendingObj.setType(keys[1]);
                    pendingObj.setUserId(Long.valueOf(keys[0]));
                    pendingObj.setSymbols(map.getValue().stream()
                            .map(UserBalanceKeys::getSymbol).collect(Collectors.toList()));
                    return pendingObj;
                }).collect(Collectors.toSet()));
        log.info("okex get future orders ended");
    }

    /**
     * 获取永续账单流水
     *
     * @param symbols
     * @return
     */
    private List<Ledger> getSwapLedgers(OkexV3ApiClient client, List<String> symbols) {
        List<Ledger> ledgers = new ArrayList<>();
        symbols.forEach(symbol -> {
            try {
                JSONArray swapLedgers = client.getSwapLedger(symbol+"-USD-SWAP", null, null, null);
                if (swapLedgers == null || swapLedgers.isEmpty()) {
                    return;
                }
                List<com.ailu.firmoffer.vo.Ledger> ledgerList = JSONArray.parseArray(swapLedgers.toJSONString(), com.ailu.firmoffer.vo.Ledger.class);
                ledgers.addAll(ledgerList.stream()
                        .filter(swapLedger-> StringUtils.hasText(swapLedger.getInstrument_id())
                                &&swapLedger.getInstrument_id().split("-").length==3)
                        .map(swapLedger -> {
                    Ledger ledger = new Ledger();
                    ledger.setAmount(new BigDecimal(swapLedger.getAmount()));
                    String[] instrumentIds = swapLedger.getInstrument_id().split("-");
                    ledger.setCurrency(instrumentIds[0]);
                    ledger.setLedger_id(swapLedger.getLedger_id());
                    ledger.setTimestamp(swapLedger.getTimestamp());
                    switch (swapLedger.getType()){
                        case "5":
                            ledger.setTypeName("充值");
                            break;
                        case "6":
                            ledger.setTypeName("提现");
                            break;
                            default:
                                ledger.setTypeName(swapLedger.getType());
                    }
                    ledger.setType(Dic.SWAP);
                    return ledger;
                }).collect(Collectors.toList()));
                Thread.sleep(800);
            } catch (Throwable e) {
                log.error("Swap ledgers get error", e);
            }
        });
        return ledgers;
    }

    /**
     * 获取杠杆账单流水
     *
     * @param symbols
     * @return
     */
    private List<Ledger> getMarginLedgers(OkexV3ApiClient client, List<String> symbols) {
        List<Ledger> ledgers = new ArrayList<>();
        symbols.forEach(symbol -> {
            try {
                //由于下方for循环会遗漏交易对市场币种，所以事先判断
                if (checkIsUnit(symbol)){
                    for (String sym:getOkexMarginSymbol()) {
                        if (getMarginList(client, ledgers, sym, symbol)) {
                            continue;
                        }
                    }
                    return;
                }
                for (String unit : MARGINUNIT) {
                    if (getMarginList(client, ledgers, symbol, unit)) {
                        continue;
                    }
                }
            } catch (Throwable e) {
                log.error("Margin ledgers get error", e);
            }
        });
        return ledgers;
    }

    private boolean checkIsUnit(String symbol) {
        for (String unit:MARGINUNIT) {
            if (symbol.equalsIgnoreCase(unit)){
                return true;
            }
        }
        return false;
    }

    private List<String> marginType = Arrays.asList("","3","4");

    private boolean getMarginList(OkexV3ApiClient client, List<Ledger> ledgers, String symbol, String unit) throws InterruptedException {
        if (symbol.equalsIgnoreCase(unit)) {
            return true;
        }
        for (String type:marginType) {
            Thread.sleep(800);
            log.info("开始查询杠杆ledger,symbol{},unit{},type{}",symbol,unit,type);
            List<UserMarginBillDto> marginLedgers = client.getMarginLedger(symbol + "-" + unit, type, null, null, null);
            if (marginLedgers == null || marginLedgers.isEmpty()) {
                continue;
            }
            ledgers.addAll(marginLedgers.stream().map(marginBillDto -> {
                Ledger ledger = new Ledger();
                ledger.setAmount(checkIsBrow(type)?new BigDecimal(marginBillDto.getAmount()).multiply(new BigDecimal("-1")):new BigDecimal(marginBillDto.getAmount()));
                ledger.setBalance(new BigDecimal(marginBillDto.getBalance()));
                ledger.setCurrency(marginBillDto.getCurrency());
                ledger.setLedger_id(checkIsBrow(type)?Long.valueOf(marginBillDto.getLedger_id()+"9991"):marginBillDto.getLedger_id());
                ledger.setTimestamp(marginBillDto.getTimestamp());
                ledger.setTypeName(marginBillDto.getType());
                ledger.setType(Dic.MARGIN);
                return ledger;
            }).collect(Collectors.toList()));
        }
        return false;
    }

    /**
     * 检查是否是借入借出
     * @param type
     * @return
     */
    private boolean checkIsBrow(String type) {
        return "3".equals(type)||"4".equals(type);
    }

    /**
     * 获取期货账单流水
     *
     * @param symbols
     * @return
     */
    private List<Ledger> getFutureLedgers(OkexV3ApiClient client, List<String> symbols) {
        List<Ledger> ledgers = new ArrayList<>();
        symbols.forEach(symbol -> {
            try {
                JSONArray futureLedgers = client.getFutureLedger(symbol);
                if (futureLedgers == null || futureLedgers.isEmpty()) {
                    return;
                }
                ledgers.addAll(futureLedgers.stream().map(futureLedger -> {
                    JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(futureLedger));
                    Ledger ledger = new Ledger();
                    ledger.setAmount(jsonObject.getBigDecimal("amount"));
                    ledger.setBalance(jsonObject.getBigDecimal("balance"));
                    ledger.setCurrency(jsonObject.getString("currency"));
                    ledger.setLedger_id(jsonObject.getLong("ledger_id"));
                    ledger.setTimestamp(jsonObject.getString("timestamp"));
                    ledger.setTypeName(jsonObject.getString("type"));
                    ledger.setType(Dic.FUTURE);
                    return ledger;
                }).collect(Collectors.toList()));
                Thread.sleep(800);
            } catch (Throwable e) {
                log.error("Spot ledgers get error", e);
            }
        });
        return ledgers;
    }

    /**
     * 获取币币账单流水
     * limit: 20/2s
     *
     * @param symbols
     * @return
     */
    private List<Ledger> getSpotLedgers(OkexV3ApiClient client, List<String> symbols) {
        List<Ledger> ledgers = new ArrayList<>();
        symbols.forEach(symbol -> {
            try {
                List<com.okcoin.okex.open.api.bean.spot.result.Ledger> spotLedgers = client.getSpotLedger(symbol, null, null, null);
                if (spotLedgers == null || spotLedgers.isEmpty()) {
                    return;
                }
                ledgers.addAll(spotLedgers.stream().map(spotLedger -> {
                    Ledger ledger = new Ledger();
                    ledger.setAmount(new BigDecimal(spotLedger.getAmount()));
                    ledger.setBalance(new BigDecimal(spotLedger.getBalance()));
                    ledger.setCurrency(spotLedger.getCurrency());
                    ledger.setLedger_id(spotLedger.getLedger_id());
                    ledger.setTimestamp(spotLedger.getTimestamp());
                    ledger.setTypeName(spotLedger.getType());
                    ledger.setType(Dic.STOCK);
                    return ledger;
                }).collect(Collectors.toList()));
                Thread.sleep(800);
            } catch (Throwable e) {
                log.error("Spot ledgers get error", e);
            }
        });
        return ledgers;
    }

    public static void main(String[] args) {
        String unitStr = Arrays.asList("BTC", "USDT").stream().collect(Collectors.joining("_"));
        System.out.println(unitStr);
        System.out.println("USD".contains(unitStr));
        System.out.println(unitStr.contains("USD"));
    }


}
