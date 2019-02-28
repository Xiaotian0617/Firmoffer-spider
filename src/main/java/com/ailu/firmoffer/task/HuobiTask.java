package com.ailu.firmoffer.task;

import com.ailu.firmoffer.dao.bean.FirmOfferKey;
import com.ailu.firmoffer.dao.bean.FirmOfferMatchHist;
import com.ailu.firmoffer.domain.HuobiMatchQuery;
import com.ailu.firmoffer.domain.HuobiSymbolResponse;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.exchange.apiclient.HuobiApiClient;
import com.ailu.firmoffer.exchange.conversion.HuobiConversion;
import com.ailu.firmoffer.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static com.ailu.firmoffer.task.Pending.huobiMatchHist;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/23 11:27
 */
@Slf4j
@Component
public class HuobiTask implements ExChangeTask {

    @Resource
    HuobiConversion huobiConversion;

    /**
     * 调用频率限制，每秒十次
     */
    @Scheduled(initialDelay = 10 * 1000, fixedDelay = 100)
    public void accounts() {
        Map<Long, FirmOfferKey> offerKeys = new HashMap<>(huobiConversion.getOfferKeys());
        offerKeys.forEach((userId, secret) -> {
            HuobiApiClient client = new HuobiApiClient(secret.getApikey(), secret.getApikeysecret());
            String results = client.getAccounts();
            huobiConversion.accountApi(userId, results);
            log.debug("HuobiTask user id accounts : {} , {} ", userId, results);
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                log.error("ERROR : {} {} results {}", e, e.getMessage(), results);
            }
        });
    }

    /**
     * 获取账户余额
     * 调用频率限制，每秒十次
     */
    @Override
    public void balances() {
        if (huobiConversion.getOfferKeys().size() > 0) {
            Map<Long, FirmOfferKey> offerKeys = new HashMap<>(huobiConversion.getOfferKeys());
            offerKeys.forEach((userId, secret) -> {
                HuobiApiClient client = new HuobiApiClient(secret.getApikey(), secret.getApikeysecret());
                //遍历火币的所有账户
                List<LinkedHashMap> accounts = huobiConversion.getHuobiAccountsIdMap(userId);
                if (accounts != null && accounts.size() > 0) {
                    for (LinkedHashMap account : accounts) {
                        String results = client.getBalance(String.valueOf(account.get("id")));
                        huobiConversion.balancesApi(String.valueOf(account.get("id")), userId, results);
                        log.debug("HuobiTask user id balances : {} , {} ", userId, results);
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            log.error("ERROR : {} {} ", e, e.getMessage());
                        }
                    }
                }
            });
        }
    }

    @Override
    public void position() {

    }

    @Override
    public void ordersHist() {

    }

    @Override
    public void matchsHist() {
        if (huobiMatchHist.size() > 0) {
            List<PendingObj> list = new ArrayList<>(huobiMatchHist);
            log.trace("订单list:{}", list);
            list.forEach(pendingObj -> {
                HuobiApiClient client = new HuobiApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret());
                //根据币种
                try {
                    getMatchBySymbols(pendingObj, client);
                } catch (Throwable e) {
                    log.error("更新HuoBi Match出错!", e);
                    return;
                }
                subtractOne(huobiMatchHist, pendingObj);
            });
        }
        //TODO 2018年11月28日21:05:46 改为一个一个移除，如果成功就移除
        // refresh(huobiMatchHist);
    }

    public void matchBySymbol(PendingObj pendingObj, String symbol) {
        HuobiApiClient client = new HuobiApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret());
        List<HuobiSymbolResponse> symbols = huobiConversion.getHuobiSymbol(symbol);
        //steem、smt 等 尚未有交易对，暂时排除
        if (symbols == null) {
            log.warn("{} 未找到交易对，跳过其交易记录更新。", symbol);
            return;
        }
        for (HuobiSymbolResponse obj : symbols) {
            getMatchBySymbol(pendingObj, client, obj, null);
        }
    }

    private void getMatchBySymbols(PendingObj pendingObj, HuobiApiClient client) {
        for (String symbol : pendingObj.getSymbols()) {
            //根据交易对
            List<HuobiSymbolResponse> symbols = huobiConversion.getHuobiSymbol(symbol);
            //steem、smt 等 尚未有交易对，暂时排除
            if (symbols == null) {
                continue;
            }
            for (HuobiSymbolResponse obj : symbols) {
                getMatchBySymbol(pendingObj, client, obj, null);
            }
        }
    }

    private void getMatchBySymbol(PendingObj pendingObj, HuobiApiClient client, HuobiSymbolResponse obj, HuobiMatchQuery query) {
        if (query == null) {
            query = new HuobiMatchQuery(obj.getSymbol(), "2018-05-21", "100");
        }
        String results = client.getClinchDeal(query);
        FirmOfferMatchHist matchs = huobiConversion.matchs(pendingObj.getUserId(), results);
        log.debug("HuobiTask user id ordersBySymbol : {} , {} ", pendingObj.getUserId(), results);
        if (matchs != null) {
            Date matchDate = matchs.getMatchDate();
            String dataStr = DateUtils.convertDateToString(matchDate);
            query.setEndDate(dataStr);
            getMatchBySymbol(pendingObj, client, obj, query);
        }
        try {
            Thread.sleep(100L);
        } catch (InterruptedException e) {
            log.error("ERROR : {} {} ", e, e.getMessage());
        }
    }

}
