package com.ailu.firmoffer.task;

import com.ailu.firmoffer.dao.bean.FirmOfferKey;
import com.ailu.firmoffer.exchange.conversion.BinanceConversion;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.account.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BinanceTask implements ExChangeTask {
    @Resource
    BinanceConversion binanceConversion;

    @Override
    public void balances() {
        if (binanceConversion.getOfferKeys().size() > 0) {
            Map<Long, FirmOfferKey> offerKeys = new HashMap<>(binanceConversion.getOfferKeys());
            offerKeys.forEach((userId, secret) -> {
                BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(secret.getApikey(), secret.getApikeysecret());
                BinanceApiRestClient client = factory.newRestClient();
                Account account = client.getAccount();
                List<String> symbols = binanceConversion.balancesApi(userId, account);
                if (symbols.size() > 0) {
                    binanceConversion.matchsHistApi(userId, client, symbols);
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

    }


}
