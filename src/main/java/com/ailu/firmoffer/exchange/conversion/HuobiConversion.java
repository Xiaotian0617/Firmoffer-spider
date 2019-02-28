package com.ailu.firmoffer.exchange.conversion;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.dao.bean.FirmOfferCoinContrast;
import com.ailu.firmoffer.dao.bean.FirmOfferExchangeBalance;
import com.ailu.firmoffer.dao.bean.FirmOfferMatchHist;
import com.ailu.firmoffer.dao.bean.FirmOfferOrderHist;
import com.ailu.firmoffer.domain.HuobiResponseApi;
import com.ailu.firmoffer.domain.HuobiSymbolResponse;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.task.HuobiTask;
import com.ailu.firmoffer.task.Pending;
import com.ailu.firmoffer.util.SendKafkaUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.ailu.firmoffer.service.MetaService.EX_CHANGE_HUOBI;
import static com.ailu.firmoffer.service.MetaService.coinContrastMap;
import static com.ailu.firmoffer.task.Pending.huobiDWH;
import static com.ailu.firmoffer.task.Pending.huobiOrderHist;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/23 12:00
 */
@Slf4j
@Component
public class HuobiConversion extends ExchangeConversion {

    @Resource
    SendKafkaUtils sendKafkaUtils;

    @Resource
    ObjectMapper objectMapper;

    @Resource
    HuobiTask huobiTask;

    /**
     * huobi交易对（查询订单用）
     */
    private Map<String, List<HuobiSymbolResponse>> huobiSymbol = new ConcurrentHashMap<>(300);

    /**
     * huobi用户所有的账户
     */
    public Map<Long, List<LinkedHashMap>> huobiAccountsIdMap = new ConcurrentHashMap<>(10);

    public static final String EX_CHANGE_API_HUOBI_ORIGIN_DISABLE = "error";

    public void accountApi(Long userId, String origin) {
        if (isDisable(origin)) {
            log.warn("huobi accountApi userid {} 未获取到正常数据 {}", userId, origin);
        } else {
            try {
                //数据转换
                HuobiResponseApi<LinkedHashMap> list = objectMapper.readValue(origin, HuobiResponseApi.class);
                if (list.getData().size() > 0) {
                    //将账户信息保存进内存中
                    huobiAccountsIdMap.put(userId, list.getData());
                }
            } catch (IOException e) {
                log.error("huobi accountApi 数据转化异常 {} {} str {}", e, e.getMessage(), origin);
            }
        }
    }

    public void balancesApi(String accountId, Long userId, String origin) {
        if (isDisable(origin)) {
            log.warn("huobi balances userid {} 未获取到正常数据 {}", userId, origin);
        } else {
            JSONObject reqJson;
            try {
                //数据转换
                reqJson = JSON.parseObject(origin);
                //存储数据
                //判断是否存在历史数据,如果没有，添加。如果存在，更新
                List<FirmOfferExchangeBalance> balanceList = getBlancesMap(userId,Dic.STOCK.toUpperCase());
                //Map<String, FirmOfferExchangeBalance> balanceMap = balanceList.stream().collect(Collectors.toMap(f -> f.getUserId() + "_" + f.getCoin(), Function.identity()));

                List<FirmOfferExchangeBalance> list = new LinkedList<>();
                JSONObject data = reqJson.getJSONObject("data");
                if (!"spot".equalsIgnoreCase(data.getString("type"))) {
                    log.warn("暂不支持火币的{}持仓抓取", data.getString("type"));
                    return;
                }
                List<JSONObject> strs = data.getJSONArray("list").toJavaList(JSONObject.class);
                List<JSONObject> trades = strs.stream().filter(v -> "trade".equals(v.getString("type"))).collect(Collectors.toList());
                List<JSONObject> frozen = strs.stream().filter(v -> "frozen".equals(v.getString("type"))).collect(Collectors.toList());
                List<JSONObject> loan = strs.stream().filter(v -> "loan".equals(v.getString("type"))).collect(Collectors.toList());
                List<String> symbols = new ArrayList<>(50);
                for (JSONObject tradesObj : trades) {
                    //因为bt1 ，bt2 是huobi历史遗留问题，现已经全部转入btc
                    if (tradesObj.getString("currency").equals("bt1") || tradesObj.getString("currency").equals("bt2")) {
                        continue;
                    }
                    FirmOfferExchangeBalance exchangeBalance = new FirmOfferExchangeBalance();
                    String symbol = tradesObj.getString("currency");
                    JSONObject frozenObj = frozen.stream().filter(f -> f.getString("currency").equals(symbol)).findFirst().get();
                    JSONObject loanObj = loan.stream().filter(f -> f.getString("currency").equals(symbol)).findFirst().orElse(new JSONObject());
                    if (loanObj.size() < 1) {
                        loanObj.put("balance", BigDecimal.ZERO);
                    }
                    exchangeBalance.setLoan(loanObj.getBigDecimal("balance").setScale(10, BigDecimal.ROUND_DOWN));
                    exchangeBalance.setAmount(frozenObj.getBigDecimal("balance").add(tradesObj.getBigDecimal("balance")));
                    exchangeBalance.setExChange(EX_CHANGE_HUOBI);
                    exchangeBalance.setSymbol(tradesObj.getString("currency"));
                    exchangeBalance.setUserId(userId);
                    //按照对照表加入coinId
                    FirmOfferCoinContrast coinId = null;
                    try {
                        coinId = coinContrastMap.values().stream().filter(coin -> ((String) tradesObj.get("currency")).toUpperCase().equals(coin.getHuobiSymbol().toUpperCase())).findFirst().get();
                    } catch (Exception e) {
                        log.debug("没有找到相应的币种对照表信息，币种 {} str {}", tradesObj.get("currency"), tradesObj);
                        continue;
                    }
                    exchangeBalance.setCoin(coinId.getCoin());
                    exchangeBalance.setType(Dic.STOCK);
                    exchangeBalance.setFreeze(frozenObj.getBigDecimal("balance"));
                    exchangeBalance.setAvailable(tradesObj.getBigDecimal("balance"));

                    String key = exchangeBalance.getUserId() + "_" + exchangeBalance.getCoin();
//                    if (balanceMap.get(key)!=null){
//                        exchangeBalance.setuTime(balanceMap.get(key).getuTime());
//                    }
                    exchangeBalance.setuTime(new Date());
                    //
                    for (FirmOfferExchangeBalance obj : balanceList) {
                        if (obj.getCoin().equals(exchangeBalance.getCoin()) && obj.getSymbol().equals(exchangeBalance.getSymbol()) && obj.getType().equals(exchangeBalance.getType())) {
                            //相同币种，判断数量，余额，冻结 变动
                            if (!obj.getAmount().setScale(10, BigDecimal.ROUND_DOWN).equals(exchangeBalance.getAmount().setScale(10, BigDecimal.ROUND_DOWN)) ||
                                    !obj.getAvailable().setScale(10, BigDecimal.ROUND_DOWN).equals(exchangeBalance.getAvailable().setScale(10, BigDecimal.ROUND_DOWN))) {
                                //||   !obj.getFreeze().setScale(10, BigDecimal.ROUND_DOWN).equals(exchangeBalance.getFreeze().setScale(10, BigDecimal.ROUND_DOWN))
                                //余额 ， 冻结 ， 变动 添加该币种到充值提现
                                symbols.add(exchangeBalance.getSymbol());
                            }
                        }
                    }
                    list.add(exchangeBalance);
                }
                //剔除usdt
//                symbols.remove("usdt");
                //批量添加
                if (balanceList.size() > 1) {
                    //批量更新
                    replaceBlance(userId, list, EX_CHANGE_HUOBI, Dic.STOCK, Dic.DELETE);
                    if (symbols.size() > 0) {
                        setOrderPending(userId, symbols);
                    }

                } else {
                    //批量添加
                    if (list.size() > 1) {
                        replaceBlance(userId, list, EX_CHANGE_HUOBI, Dic.STOCK, Dic.INSERT);
                        //历史没有订单记录，现有余额，添加历史订单查询 活跃订单查询
                        setOrderPending(userId, symbols);
                    }
                }

                list.forEach(firmOfferExchangeBalance -> {
                    Date date = firmOfferExchangeBalance.getuTime();
                    if (date == null) {
                        return;
                    }
                    long time = System.currentTimeMillis() - date.getTime();
                    if (Long.compare(time, 60000L) < 0) {
                        Long uId = firmOfferExchangeBalance.getUserId();
                        log.debug("发现{}币种资产发生变更，开始更新其交易记录！", firmOfferExchangeBalance.getSymbol());
                        huobiTask.matchBySymbol(Pending.huobiPendingObjMap.get(uId), firmOfferExchangeBalance.getSymbol());
                    }
                });

            } catch (Exception e) {
                log.error("huobi balancesApi 数据转化异常 {} {} str {}", e, e.getMessage(), origin);
            }
        }
    }


    public void orders(Long userId, String origin) {
        if (isDisable(origin)) {
            log.debug("huobi orders userid {} 未获取到正常数据 {}", userId, origin);
        } else {
            JSONObject data;
            try {
                //数据转换
                data = JSON.parseObject(origin);
                //存储数据
                List<FirmOfferOrderHist> list = new LinkedList<>();
                List<JSONObject> strs = data.getJSONArray("data").toJavaList(JSONObject.class);

                for (JSONObject json : strs) {
                    //判断订单状态
                    int orderStatus;
                    switch (json.getString("state")) {
                        case "pre-submitted":
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_NOT_DEAL;
                            break;
                        case "submitted":
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_NOT_DEAL;
                            break;
                        case "partial-filled":
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_INCOMPLETE_DEAL;
                            break;
                        case "partial-canceled":
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_COMPLETELY_DEAL;
                            break;
                        case "filled":
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_COMPLETELY_DEAL;
                            break;
                        case "canceled":
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_CANCEL;
                            break;
                        default:
                            orderStatus = Dic.EX_CHANGE_API_ORDERS_HISTORY_ORDER_STATUS_DEFAULT;
                            break;
                    }
                    FirmOfferOrderHist hist = new FirmOfferOrderHist();
                    hist.setOrderStatus(orderStatus);
                    hist.setId(json.getString("id"));
                    hist.setType(json.getString("type"));
                    hist.setPrice(json.getBigDecimal("price"));
                    hist.setSide(json.getString("type").split("-")[0]);
                    hist.setAmount(json.getBigDecimal("amount"));
                    hist.setFieldAmount(json.getBigDecimal("field-amount"));
                    if (isBigDecimalZero(hist.getFieldAmount())) {
                        hist.setFieldPrice(hist.getPrice());
                    } else {
                        hist.setFieldPrice(json.getBigDecimal("field-cash-amount").divide(json.getBigDecimal("field-amount"), 16, BigDecimal.ROUND_DOWN));
                    }
                    hist.setOrderDate((json.getTimestamp("created-at")));
                    hist.setUserId(userId);
                    hist.setExChange(EX_CHANGE_HUOBI);
                    hist.setTradingOn(json.getString("symbol"));
                    list.add(hist);
                }
                //ON DUPLICATE KEY UPDATE 储存数据
                replaceOrder(userId, list, EX_CHANGE_HUOBI, Dic.STOCK, Dic.REPLACE);
            } catch (Exception e) {
                log.error("huobi orders 数据转化异常 {} {} str {}", e, e.getMessage(), origin);
            }
        }
    }


    private void setOrderPending(Long userId, List<String> symbol) {
        huobiOrderHist.add(new PendingObj(getOfferKeys().get(userId), userId, symbol));
        huobiDWH.add(new PendingObj(getOfferKeys().get(userId), userId, symbol));
    }


    public boolean isDisable(String src) {
        if (src.contains(EX_CHANGE_API_HUOBI_ORIGIN_DISABLE)) {
            return true;
        }
        if (src.contains("\"data\":[]")) {
            return true;
        }
        if (isBlank(src)) {
            return true;
        }
        return false;
    }

    private boolean isBlank(String src) {
        return null == src || "".equals(src.trim());
    }

    private boolean isEquals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    private boolean isBigDecimalZero(BigDecimal bigDecimal) {
        return isEquals(bigDecimal, null) || isEquals(bigDecimal, BigDecimal.ZERO) || bigDecimal.compareTo(BigDecimal.ZERO) == 0;
    }

    public FirmOfferMatchHist matchs(Long userId, String results) {
        if (isDisable(results)) {
            log.debug("huobi matchs userid {} 未获取到正常数据 {}", userId, results);
        } else {
            JSONObject data;
            try {
                //数据转换
                data = JSON.parseObject(results);
                //存储数据
                List<FirmOfferMatchHist> list = new LinkedList<>();
                List<JSONObject> strs = data.getJSONArray("data").toJavaList(JSONObject.class);
                strs.forEach(json -> {
                    FirmOfferMatchHist hist = new FirmOfferMatchHist();
                    hist.setId(json.getLong("id"));
                    hist.setType(json.getString("type"));
                    hist.setPrice(json.getBigDecimal("price"));
                    hist.setOrderId(json.getLong("order-id"));
                    hist.setMatchId(json.getLong("match-id"));
                    hist.setFieldFees(json.getBigDecimal("filled-fees"));
                    hist.setFieldAmount(json.getBigDecimal("filled-amount"));
                    hist.setSource(json.getString("source"));
                    hist.setMatchDate((json.getTimestamp("created-at")));
                    hist.setUserId(userId);
                    hist.setExChange(EX_CHANGE_HUOBI);
                    hist.setSymbol(json.getString("symbol"));
                    hist.setMatchType(Dic.STOCK);
                    list.add(hist);
                });
                //ON DUPLICATE KEY UPDATE 储存数据
                replaceMatch(userId, list, EX_CHANGE_HUOBI, Dic.STOCK, Dic.REPLACE);
                if (list.size() > 1) {
                    return list.get(list.size() - 1);
                }
            } catch (Exception e) {
                log.error("huobi match 数据转化异常 {} {} str {}", e, e.getMessage(), results);
            }
        }
        return null;
    }

    public List<LinkedHashMap> getHuobiAccountsIdMap(Long userId) {
        return huobiAccountsIdMap.getOrDefault(userId, Collections.EMPTY_LIST);
    }

    public void setHuobiAccountsIdMap(Map<Long, List<LinkedHashMap>> huobiAccountsIdMap) {
        this.huobiAccountsIdMap = huobiAccountsIdMap;
    }

    public List<HuobiSymbolResponse> getHuobiSymbol(String symbol) {
        return huobiSymbol.getOrDefault(symbol, Collections.EMPTY_LIST);
    }

    public void setHuobiSymbol(Map<String, List<HuobiSymbolResponse>> huobiSymbol) {
        this.huobiSymbol = huobiSymbol;
    }

    public Integer getHuobiSymbolSize() {
        return huobiSymbol.size();
    }
}
