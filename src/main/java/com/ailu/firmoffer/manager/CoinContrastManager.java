package com.ailu.firmoffer.manager;

import com.ailu.firmoffer.dao.mapper.ext.FirmOfferCoinContrastsExt;
import com.ailu.firmoffer.domain.CoinContrasts;
import com.ailu.firmoffer.service.MetaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ailu.firmoffer.service.MetaService.*;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/7 16:04
 */
@Slf4j
@Component
public class CoinContrastManager {

    @Resource
    MetaService metaService;

    @Resource
    FirmOfferCoinContrastsExt firmOfferCoinContrastsExt;

    /**
     * 币种对照集合
     */
    public static Map<String, List<CoinContrasts>> concurrentsHashMap = new ConcurrentHashMap<>();

    public static Map<String, CoinContrasts> huobiContrasts = new ConcurrentHashMap<>();

    public static Map<String, CoinContrasts> bitfinexContrasts = new ConcurrentHashMap<>();

    public static Map<String, CoinContrasts> okexContrasts = new ConcurrentHashMap<>();

    public static Map<String, CoinContrasts> biboxContrasts = new ConcurrentHashMap<>();

    public static Map<String, CoinContrasts> binanceContrasts = new ConcurrentHashMap<>();

    public static Map<String, CoinContrasts> bitmexContrasts = new ConcurrentHashMap<>();

    /**
     * 从数据库初始化各个币种对照表
     * SELECT p.id,p.symbol,p.coin,p.chinese,c.symbol childSymbol,c.ex_change exChange
     * FROM firm_offer_coin_contrasts p LEFT JOIN firm_offer_coin_contrasts c ON p.id = c.parent
     * WHERE c.ex_change IS NOT NULL AND p.parent is NOT NULL AND p.coin IS NOT NULL
     */
    public void init() {
        //TODO 初始化币种对照表
        //List<CoinContrasts> list = metaService.getFileToObject("","coinContrasts.json",CoinContrasts.class);
        List<CoinContrasts> list = gerCoinContrasts();
        concurrentsHashMap = list.stream().collect(Collectors.groupingBy(CoinContrasts::getExChange, Collectors.toList()));
        log.info("更新了 {} 个交易所的币种对照表", concurrentsHashMap.size());
        //初始化所有交易所对照表
        //初始化bitfinex
        if (null != concurrentsHashMap.get(EX_CHANGE_BITFINEX)) {
            bitfinexContrasts = concurrentsHashMap.get(EX_CHANGE_BITFINEX).stream().collect(Collectors.toMap(CoinContrasts::getChildSymbol, Function.identity(), (o1, o2) -> o1));
        }
        //初始化huobi
        if (null != concurrentsHashMap.get(EX_CHANGE_HUOBI)) {
            huobiContrasts = concurrentsHashMap.get(EX_CHANGE_HUOBI).stream().collect(Collectors.toMap(CoinContrasts::getChildSymbol, Function.identity(), (o1, o2) -> o1));
        }
        //初始化bibox
        if (null != concurrentsHashMap.get(EX_CHANGE_BIBOX)) {
            biboxContrasts = concurrentsHashMap.get(EX_CHANGE_BIBOX).stream().collect(Collectors.toMap(CoinContrasts::getChildSymbol, Function.identity(), (o1, o2) -> o1));
        }
        //初始化binance
        if (null != concurrentsHashMap.get(EX_CHANGE_BINANCE)) {
            binanceContrasts = concurrentsHashMap.get(EX_CHANGE_BINANCE).stream().collect(Collectors.toMap(CoinContrasts::getChildSymbol, Function.identity(), (o1, o2) -> o1));
        }
        //初始化okex
        if (null != concurrentsHashMap.get(EX_CHANGE_OKEX)) {
            okexContrasts = concurrentsHashMap.get(EX_CHANGE_OKEX).stream().collect(Collectors.toMap(CoinContrasts::getChildSymbol, Function.identity(), (o1, o2) -> o1));
        }
        //初始化bitmex
        if (null != concurrentsHashMap.get(EX_CHANGE_BITMEX)) {
            bitmexContrasts = concurrentsHashMap.get(EX_CHANGE_BITMEX).stream().collect(Collectors.toMap(CoinContrasts::getSymbol, Function.identity(), (o1, o2) -> o1));
        }
        log.info("更新了 币种对照成功，共更新了 bitfinex {} ， huobi {} ，bibox {} ， binance {} ，okex {},bitmex{}",
                bitfinexContrasts.size(), huobiContrasts.size(), biboxContrasts.size(), binanceContrasts.size(),
                okexContrasts.size(), bitmexContrasts.size());
    }

    private List<CoinContrasts> gerCoinContrasts() {
        return firmOfferCoinContrastsExt.getCoinContrastsTable();
    }


}
