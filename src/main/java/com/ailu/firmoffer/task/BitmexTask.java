package com.ailu.firmoffer.task;

import com.ailu.firmoffer.dao.bean.FirmOfferKey;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.exchange.apiclient.BitmexApiClient;
import com.ailu.firmoffer.exchange.conversion.BitmexConversion;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.bitmex.dto.account.BitmexMarginAccount;
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder;
import org.knowm.xchange.bitmex.dto.trade.BitmexPosition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

import static com.ailu.firmoffer.task.Pending.bitmexOrderHist;

/**
 * NOTE:
 *
 * @Version 1.0
 * @Since JDK1.8
 * @Author mr.wang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2018/12/3 17:04
 */
@Slf4j
@Component
public class BitmexTask implements ExChangeTask {

    @Resource
    private BitmexConversion bitmexConversion;

    @Value("${spring.proxy.enable}")
    public boolean proxyEnable;

    @Value("${spring.proxy.url}")
    public String url;
    @Value("${spring.proxy.port}")
    public String port;

    @Override
    public void balances() {
        Map<Long, FirmOfferKey> offerKeys = new HashMap<>(bitmexConversion.getOfferKeys());
        Long sleepTime = 30000L;
        if (offerKeys.isEmpty()) {
            return;
        }
        offerKeys.forEach((userId, secret) -> {
            BitmexApiClient client = new BitmexApiClient(secret.getApikey(), secret.getApikeysecret(), secret.getPassphrase(), proxyEnable, url, port);
            try {
                List<BitmexMarginAccount> wallets = client.getBitmexMarginAccountsStatus();
                bitmexConversion.addAccounts(userId, wallets);
                Thread.sleep(sleepTime);
            } catch (Exception e) {
                log.error("Bitmex balance error", e);
            }
        });
    }

    @Override
    public void position() {
//        Map<Long, FirmOfferKey> offerKeys = new HashMap<>(bitmexConversion.getOfferKeys());
//        Long sleepTime = 20000L;
//        if (offerKeys.isEmpty()) {
//            return;
//        }
//        offerKeys.forEach((userId, secret) -> {
//            BitmexApiClient client = new BitmexApiClient(secret.getApikey(), secret.getApikeysecret(), secret.getPassphrase(), proxyEnable, url, port);
//            try {
//                List<BitmexPosition> futurePositions = client.getFuturePositions();
//                bitmexConversion.addPositions(userId, futurePositions);
//                Thread.sleep(sleepTime / offerKeys.size());
//            } catch (Exception e) {
//                log.error("Bitmex position error", e);
//            }
//        });
    }

    @Override
    public void ordersHist() {
//        if (bitmexOrderHist.size() <= 0) {
//            return;
//        }
//        Set<PendingObj> list = new HashSet<>(bitmexOrderHist);
//        log.trace("订单list:{}", list);
//        Long sleepTime = 10000L;
//        list.forEach(pendingObj -> {
//            BitmexApiClient client = new BitmexApiClient(pendingObj.getKey().getApikey(), pendingObj.getKey().getApikeysecret(), pendingObj.getKey().getPassphrase(), proxyEnable, url, port);
//            try {
//                List<BitmexPrivateOrder> futureOrders = client.getFutureOrders();
//                bitmexConversion.addOrders(pendingObj.getUserId(), futureOrders);
//                Thread.sleep(sleepTime / list.size());
//            } catch (Exception e) {
//                log.error("Bitmex orders error", e);
//            }
//        });
    }

    @Override
    public void matchsHist() {

    }
}
