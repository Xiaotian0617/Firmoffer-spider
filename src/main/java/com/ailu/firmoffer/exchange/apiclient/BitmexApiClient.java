package com.ailu.firmoffer.exchange.apiclient;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bitmex.BitmexExchange;
import org.knowm.xchange.bitmex.dto.account.BitmexMarginAccount;
import org.knowm.xchange.bitmex.dto.marketdata.BitmexPrivateOrder;
import org.knowm.xchange.bitmex.dto.trade.BitmexPosition;
import org.knowm.xchange.bitmex.service.BitmexAccountService;
import org.knowm.xchange.bitmex.service.BitmexTradeService;
import org.knowm.xchange.dto.account.AccountInfo;
import org.knowm.xchange.dto.account.Wallet;
import org.knowm.xchange.service.marketdata.MarketDataService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * NOTE:
 * Okex v3 版本API接口
 *
 * @Version 1.0
 * @Since JDK1.8
 * @Author mr.wang
 * @Company 洛阳艾鹿网络有限公司
 * @Date 2018/11/9 15:04
 */
@Slf4j
@Component
public class BitmexApiClient {

    private ExchangeSpecification exchangeSpecification;

    private BitmexAccountService accountService;

    private BitmexTradeService tradeService;

    private MarketDataService marketDataService;

    private Exchange bitmex;

    public BitmexApiClient() {
    }

    public BitmexApiClient(String apiKey, String secretKey, String passphrase, boolean proxyEnable, String url, String port) {
        this.exchangeSpecification = config(apiKey, secretKey, passphrase, proxyEnable, url, port);
        this.bitmex = ExchangeFactory.INSTANCE.createExchange(this.exchangeSpecification);
        this.accountService = new BitmexAccountService(bitmex);
        this.tradeService = new BitmexTradeService(bitmex);
        this.marketDataService = this.bitmex.getMarketDataService();
    }

    public ExchangeSpecification config(String apiKey, String secretKey, String passphrase, boolean proxyEnable, String url, String port) {
        ExchangeSpecification exSpec = new BitmexExchange().getDefaultExchangeSpecification();
        exSpec.setApiKey(apiKey);
        exSpec.setSecretKey(secretKey);
        if (proxyEnable) {
            exSpec.setProxyHost(url);
            exSpec.setProxyPort(Integer.valueOf(port));
        }
        return exSpec;
    }

    /**
     * 获取用户spot账户信息
     */
    public AccountInfo getAccounts() {
        AccountInfo accounts = null;
        try {
            accounts = accountService.getAccountInfo();
        } catch (IOException e) {
            e.printStackTrace();
        }
        toResultString("accounts", accounts);
        return accounts;
    }


    /**
     * 获取期货订单列表
     *
     * @return
     */
    public List<BitmexPrivateOrder> getFutureOrders() {
        try {
            List<BitmexPrivateOrder> result = tradeService.getBitmexOrders();
            toResultString("Get-Orders", result);
            return result;
        } catch (Exception e) {
            log.error("获取期货订单列表出错！", e);
        }
        return Collections.EMPTY_LIST;

    }

    //    /**
//     * 获取现货账单流水
//     */
//    public List<Ledger> getLedgersByCurrency(String symbol) {
//        try {
//            final List<Ledger> ledgers = this.spotAccountAPIService.getLedgersByCurrency(symbol, null, null, "100");
//            this.toResultString("ledges", ledgers);
//            return ledgers;
//        } catch (Exception e) {
//            log.error("获取现货账单流水出错！", e);
//        }
//        return Collections.EMPTY_LIST;
//
//    }
//
//    /**
//     * 获取期货账单流水
//     *
//     * @param symbol
//     * @return
//     */
//    public JSONArray getAccountsLedgerByCurrency(String symbol) {
//        try {
//            JSONArray ledger = futuresTradeAPIService.getAccountsLedgerByCurrency(symbol);
//            toResultString("Ledger", ledger);
//            return ledger;
//        } catch (Exception e) {
//            log.error("获取期货账单流水出错！", e);
//        }
//        return new JSONArray();
//    }
//
    public List<Wallet> getWallet() {
        try {
            List<Wallet> result = this.getWallet();
            this.toResultString("wallet", result);
            return result;
        } catch (Exception e) {
            log.error("获取钱包账户出错！", e);
        }
        return Collections.EMPTY_LIST;
    }

    public List<BitmexMarginAccount> getBitmexMarginAccountsStatus() {
        try {
            List<BitmexMarginAccount> bitmexMarginAccountsStatus = accountService.getBitmexMarginAccountsStatus();
            this.toResultString("wallet status", bitmexMarginAccountsStatus);
            return bitmexMarginAccountsStatus;
        } catch (Exception e) {
            log.error("获取钱包状态出错！", e);
        }
        return Collections.EMPTY_LIST;
    }

    //
//
    public List<BitmexPosition> getFuturePositions() throws IOException {
        List<BitmexPosition> positions = tradeService.getBitmexPositions();
        this.toResultString("futurePosition", positions);
        return positions;
    }

    public void toResultString(String flag, Object object) {
        if (log.isDebugEnabled()) {
            StringBuilder su = new StringBuilder();
            su.append("\n").append("<*> ").append(flag).append(": ").append(JSON.toJSONString(object));
            log.debug(su.toString());
        }
    }
}
