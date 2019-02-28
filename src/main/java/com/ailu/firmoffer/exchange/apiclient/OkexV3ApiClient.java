package com.ailu.firmoffer.exchange.apiclient;

import com.ailu.firmoffer.config.Dic;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okcoin.okex.open.api.bean.account.param.Transfer;
import com.okcoin.okex.open.api.bean.account.result.Wallet;
import com.okcoin.okex.open.api.bean.spot.result.Account;
import com.okcoin.okex.open.api.bean.spot.result.Ledger;
import com.okcoin.okex.open.api.bean.spot.result.OrderInfo;
import com.okcoin.okex.open.api.bean.spot.result.UserMarginBillDto;
import com.okcoin.okex.open.api.config.APIConfiguration;
import com.okcoin.okex.open.api.enums.I18nEnum;
import com.okcoin.okex.open.api.service.account.AccountAPIService;
import com.okcoin.okex.open.api.service.account.impl.AccountAPIServiceImpl;
import com.okcoin.okex.open.api.service.futures.FuturesTradeAPIService;
import com.okcoin.okex.open.api.service.futures.impl.FuturesTradeAPIServiceImpl;
import com.okcoin.okex.open.api.service.margin.MarginAccountAPIService;
import com.okcoin.okex.open.api.service.margin.MarginOrderAPIService;
import com.okcoin.okex.open.api.service.margin.impl.MarginAccountAPIServiceImpl;
import com.okcoin.okex.open.api.service.margin.impl.MarginOrderAPIServiceImpl;
import com.okcoin.okex.open.api.service.spot.SpotAccountAPIService;
import com.okcoin.okex.open.api.service.spot.SpotOrderAPIServive;
import com.okcoin.okex.open.api.service.spot.impl.SpotAccountAPIServiceImpl;
import com.okcoin.okex.open.api.service.spot.impl.SpotOrderApiServiceImpl;
import com.okcoin.okex.open.api.service.swap.SwapTradeAPIService;
import com.okcoin.okex.open.api.service.swap.impl.SwapTradeAPIServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
public class OkexV3ApiClient {

    private APIConfiguration apiConfiguration;

    private SpotAccountAPIService spotAccountAPIService;

    private SpotOrderAPIServive spotOrderAPIServive;

    private FuturesTradeAPIService futuresTradeAPIService;

    private AccountAPIService accountAPIService;

    private SwapTradeAPIService swapTradeAPIService;

    private MarginAccountAPIService marginAccountAPIService;

    private MarginOrderAPIService marginOrderAPIService;

    public OkexV3ApiClient() {
    }

    public OkexV3ApiClient(String apiKey, String secretKey, String passphrase, boolean proxyEnable, String url, String port) {
        this.apiConfiguration = config(apiKey, secretKey, passphrase, proxyEnable, url, port);
        this.spotAccountAPIService = new SpotAccountAPIServiceImpl(this.apiConfiguration);
        this.futuresTradeAPIService = new FuturesTradeAPIServiceImpl(this.apiConfiguration);
        this.spotOrderAPIServive = new SpotOrderApiServiceImpl(this.apiConfiguration);
        this.accountAPIService = new AccountAPIServiceImpl(this.apiConfiguration);
        this.swapTradeAPIService = new SwapTradeAPIServiceImpl(this.apiConfiguration);
        this.marginAccountAPIService = new MarginAccountAPIServiceImpl(this.apiConfiguration);
        this.marginOrderAPIService = new MarginOrderAPIServiceImpl(this.apiConfiguration);
    }

    public APIConfiguration config(String apiKey, String secretKey, String passphrase, boolean proxyEnable, String url, String port) {
        final APIConfiguration config = new APIConfiguration();
        config.setEndpoint("https://www.okex.com/");
        // apiKey，api注册成功后页面上有
        config.setApiKey(apiKey);
        // secretKey，api注册成功后页面上有
        config.setSecretKey(secretKey);
        config.setPassphrase(passphrase);
        config.setPrint(true);
        config.setI18n(I18nEnum.SIMPLIFIED_CHINESE);
        if (proxyEnable) {
            log.info("Okex 启动代理模式！");
            config.setEnableProxy(true);
            config.setProxyUrl(url);
            config.setProxyPort(Integer.valueOf(port));
        }
        return config;
    }

    /**
     * 获取用户spot账户信息
     */
    public List<Account> getSpotAccounts() {
        final List<Account> accounts = spotAccountAPIService.getAccounts();
        toResultString("spotAccounts", accounts);
        return accounts;
    }

    /**
     * 获取合约账户信息
     *
     * @return
     */
    public JSONObject getFutureAccounts() {
        JSONObject accounts = futuresTradeAPIService.getAccounts();
        toResultString("futureAccounts", accounts);
        return accounts;
    }

    /**
     * 获取现货订单列表
     *
     * @param product
     * @return
     */
    public List<OrderInfo> getSpotOrders(String product) {
        try {
            final List<OrderInfo> orderInfoList = spotOrderAPIServive.getOrders(product, "all", null, null, "100");
            this.toResultString("orderInfoList", orderInfoList);
            return orderInfoList;
        } catch (Exception e) {
            if (!e.getMessage().contains("30032")){
                log.error("获取现货订单列表出错！", e);
            }
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * 获取期货订单列表
     *
     * @param instrumentId 合约ID，如BTC-USD-180213
     * @return
     */
    public JSONObject getFutureOrders(String instrumentId, int status,Long userId) {
        try {
            JSONObject result = futuresTradeAPIService.getOrders(instrumentId, status, 0, 0, 100);
            toResultString("Get-Orders", result);
            return result;
        } catch (Exception e) {
            log.error("获取期货订单列表出错！userId:"+userId, e);
        }
        return new JSONObject();

    }

    public List<Wallet> getWallet() {
        try {
            List<Wallet> result = this.accountAPIService.getWallet();
            this.toResultString("wallet", result);
            return result;
        } catch (Exception e) {
            log.error("获取钱包账户出错！", e);
        }
        return Collections.EMPTY_LIST;
    }


    public JSONArray getDepositHistory() {
        try {
            JSONArray result = this.accountAPIService.getDepositHistory();
            this.toResultString("deposit", result);
            return result;
        } catch (Exception e) {
            log.error("获取提现历史出错！", e);
        }
        return new JSONArray();
    }

    public JSONObject transfer(Transfer transfer) {
        try {
            JSONObject result = this.accountAPIService.transfer(transfer);
            this.toResultString("transfer", result);
            return result;
        } catch (Exception e) {
            log.error("获取资金划转出错！", e);
        }
        return new JSONObject();
    }

    public List<com.okcoin.okex.open.api.bean.account.result.Ledger> getWalletLedger() {
        try {
            List<com.okcoin.okex.open.api.bean.account.result.Ledger> result = this.accountAPIService.getLedger(null,null,null,null,100);
            this.toResultString("transfer", result);
            return result.stream().map(ledger -> {
                ledger.setType(Dic.WALLET);
                ledger.setTypeName(BigDecimal.ZERO.compareTo(ledger.getAmount())<0?"充值":"提现");
                return ledger;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Okex get ledger error", e);
        }
        return Collections.EMPTY_LIST;
    }

    public List<Ledger> getSpotLedger(String currency, String from, String to, String limit) {
        try {
            List<Ledger> result = this.spotAccountAPIService.getLedgersByCurrency(currency, from, to, limit);
            this.toResultString("transfer", result);
            return result;
        } catch (Exception e) {
            log.error("Okex get spot ledger error", e);
        }
        return Collections.EMPTY_LIST;
    }

    public List<UserMarginBillDto> getMarginLedger(String product,
                                                   String type,
                                                   String from,
                                                   String to,
                                                   String limit) {
        try {
            List<UserMarginBillDto> result = this.marginAccountAPIService.getLedger(product, type, from, to, limit);
            this.toResultString("transfer", result);
            return result;
        } catch (Exception e) {
            if (!e.getMessage().contains("30032")){
                log.error("Okex get margin ledger error , " + e.getMessage());
            }
        }
        return Collections.EMPTY_LIST;
    }

    public JSONArray getSwapLedger(String currency, Integer from, Integer to, Integer limit) {
        try {
            JSONArray result = this.swapTradeAPIService.getLegers(currency, from, to, limit);
            this.toResultString("transfer", result);
            return result;
        } catch (Exception e) {
            if (!e.getMessage().contains("30032")){
                log.error("Okex get swap ledger error", e);
            }
        }
        return new JSONArray();
    }

    public JSONArray getFutureLedger(String currency) {
        try {
            JSONArray result = this.futuresTradeAPIService.getAccountsLedgerByCurrency(currency);
            this.toResultString("transfer", result);
            return result;
        } catch (Exception e) {
            if (!e.getMessage().contains("30032")){
                log.error("Okex get swap ledger error", e);
            }
        }
        return new JSONArray();
    }


    public void toResultString(String flag, Object object) {
        if (log.isDebugEnabled()) {
            StringBuilder su = new StringBuilder();
            su.append("\n").append("<*> ").append(flag).append(": ").append(JSON.toJSONString(object));
            log.debug(su.toString());
        }
    }

    public JSONObject getFuturePositions() {
        JSONObject positions = futuresTradeAPIService.getPositions();
        this.toResultString("futurePosition", positions);
        return positions;
    }

    public JSONObject getSwapPositions(String instrumentId) {
        JSONObject positions = swapTradeAPIService.getInstrumentPosition(instrumentId);
        this.toResultString("swapPosition", positions);
        return positions;
    }

    public JSONArray getSwapPositions() {
        JSONArray positions = swapTradeAPIService.getPosition();
        this.toResultString("swapPosition", positions);
        return positions;
    }

    public JSONObject getSwapAccount() {
        JSONObject account = swapTradeAPIService.getAccount();
        this.toResultString("swapAccount", account);
        return account;
    }

    public JSONObject getSwapOrders(String instrumentId, int status) {
        try {
            JSONObject orders = swapTradeAPIService.getOrders(instrumentId, status, null, null, null);
            this.toResultString("swapOrders", orders);
            return orders;
        }catch (Throwable e){
            if (!e.getMessage().contains("30032")){
                log.error("Okex getSwapOrders error", e);
            }
        }
        return new JSONObject();
    }

    public List<Map<String, Object>> getMarginAccount() {
        List<Map<String, Object>> account = marginAccountAPIService.getAccounts();
        this.toResultString("marginAccount", account);
        return account;
    }

    public List<OrderInfo> getMarginOrders(String instrumentId, String status, String from, String to, String limit) {
        try {
            List<OrderInfo> account = marginOrderAPIService.getOrders(instrumentId, status, from, to, limit);
            this.toResultString("marginOrders", account);
            return account;
        }catch (Throwable e){
            if (!e.getMessage().contains("30032")){
                log.error("Okex getMarginOrders error", e);
            }
        }
       return Collections.EMPTY_LIST;
    }
}
