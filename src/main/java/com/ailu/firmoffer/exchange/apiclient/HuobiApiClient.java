package com.ailu.firmoffer.exchange.apiclient;

import com.ailu.firmoffer.domain.HuobiMatchQuery;
import com.ailu.firmoffer.domain.HuobiOrdersQuery;
import com.ailu.firmoffer.exchange.signature.HuobiSignature;
import com.ailu.firmoffer.util.JsonUtil;
import com.ailu.firmoffer.util.OkHttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/21 9:19
 */
@Slf4j
public class HuobiApiClient {

    public static final String HUOBI_PRO_API_HOST = "api.huobipro.com";
    public static final String HUOBI_HADAX_API_HOST = "api.hadax.com";
    String API_HOST;
    String API_URL;
    final String accessKeyId;
    final String accessKeySecret;

    /**
     * 创建一个ApiClient实例
     *
     * @param accessKeyId     AccessKeyId
     * @param accessKeySecret AccessKeySecret
     */
    public HuobiApiClient(String accessKeyId, String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.API_URL = "https://" + HUOBI_PRO_API_HOST;
        this.API_HOST = HUOBI_PRO_API_HOST;
    }

    /**
     * 创建一个ApiClient实例
     *
     * @param accessKeyId     AccessKeyId
     * @param accessKeySecret AccessKeySecret
     */
    public HuobiApiClient(String accessKeyId, String accessKeySecret, String exChange) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.API_URL = "https://" + exChange;
        if (HUOBI_PRO_API_HOST.equals(exChange)) {
            this.API_HOST = HUOBI_PRO_API_HOST;
        } else {
            this.API_HOST = HUOBI_HADAX_API_HOST;
        }
    }

    /**
     * 判断查询哪个交易所
     *
     * @param exChange
     * @return true：pro    false：hadax
     */
    private boolean exChangeType(String exChange) {
        return (HUOBI_PRO_API_HOST.equals(exChange));
    }

    /**
     * 查询所有账户信息
     *
     * @return .
     */
    public String getAccounts() {
        String resp =
                get("/v1/account/accounts", null);
        return resp;
    }

    /**
     * 获取用户的账户余额
     *
     * @param id 账户id
     * @return
     */
    public String getBalance(String id) {
        String url = "/v1/account/accounts/" + id + "/balance";
        String resp = get(url, null);
        return resp;
    }

    /**
     * 查询某个订单详情
     *
     * @param id 订单id
     * @return
     */
    public String getOrderstatus(String id) {
        return get("/v1/order/orders/" + id, null);
    }

    /**
     * 查询某个订单的成交明细
     *
     * @param id 订单id
     * @return
     */
    public String getMatchresults(String id) {
        return get("/v1/order/orders/" + id + "/matchresults", null);
    }

    /**
     * 查询当前委托、历史委托
     *
     * @param query 查询条件
     * @return
     */
    public String getMatchresults(HuobiOrdersQuery query) {
        HashMap map = new HashMap(8);
        map.put("symbol", query.getSymbol());
        map.put("types", query.getTypes());
        map.put("start-date", query.getStartDate());
        map.put("end-date", query.getEndDate());
        map.put("states", query.getStates());
        map.put("from", query.getFrom());
        map.put("direct", query.getDirect());
        map.put("size", query.getSize());
        return get("/v1/order/orders", map);
    }

    /**
     * 查询当前成交、历史成交
     *
     * @param query
     * @return
     */
    public String getClinchDeal(HuobiMatchQuery query) {
        HashMap map = new HashMap(8);
        map.put("symbol", query.getSymbol());
        map.put("types", query.getTypes());
        map.put("start-date", query.getStartDate());
        map.put("end-date", query.getEndDate());
        map.put("from", query.getFrom());
        map.put("direct", query.getDirect());
        map.put("size", query.getSize());
        return get("/v1/order/matchresults", map);
    }

    /**
     * 获取借贷订单信息
     *
     * @param query
     * @return
     */
    public String getMarginOrders(HuobiOrdersQuery query) {
        HashMap map = new HashMap(8);
        map.put("symbol", query.getSymbol());
        map.put("states", query.getStates());
        map.put("start-date", query.getStartDate());
        map.put("end-date", query.getEndDate());
        map.put("from", query.getFrom());
        map.put("direct", query.getDirect());
        map.put("size", query.getSize());
        return get("/v1/margin/loan-orders", map);
    }

    /**
     * 借贷账户详情
     *
     * @param symbol
     * @return
     */
    public String getMarginAccountsBalance(String symbol) {
        HashMap map = new HashMap(1);
        map.put("symbol", symbol);
        return get("/v1/margin/accounts/balance", map);
    }

    /**
     * 借贷账户详情
     *
     * @return
     */
    public String getMarginAccountsBalance() {
        return get("/v1/margin/accounts/balance", null);
    }

    /**
     * 查询虚拟币充提记录
     *
     * @param query
     * @return
     */
    public String getDepositWithdraw(HuobiOrdersQuery query) {
        HashMap map = new HashMap(4);
        map.put("currency", query.getCurrency());
        map.put("type", query.getTypes());
        map.put("from", query.getFrom());
        map.put("size", query.getSize());
        return get("/v1/query/deposit-withdraw", map);
    }

    public String getLoanBalance(String symbol) {
        HashMap map = new HashMap(1);
        map.put("symbol", symbol);
        return get("/v1/margin/accounts/balance", map);
    }

    /**
     * send a GET request
     *
     * @param uri
     * @param params
     * @return
     */
    private String get(String uri, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        return call("GET", uri, null, params);
    }

    /**
     * send a POST request
     *
     * @param uri
     * @param object
     * @return
     */
    private String post(String uri, Object object) {
        return call("POST", uri, object, new HashMap<String, String>());
    }

    /**
     * call api by endpoint
     *
     * @param method
     * @param uri
     * @param object
     * @param params
     * @return
     */
    private String call(String method, String uri, Object object, Map<String, String> params) {
        log.trace("准备开始调用 Huobi API 调用方法 {} url {}", method, uri);
        HuobiSignature signature = new HuobiSignature();
        signature.createSignature(this.accessKeyId, this.accessKeySecret, method, API_HOST, uri, params);
        try {
            Request.Builder builder;
            if (OkHttpClientUtil.REQUEST_POST.equals(method)) {
                RequestBody body = RequestBody.create(OkHttpClientUtil.JSON, JsonUtil.writeValue(object));
                builder = new Request.Builder().url(API_URL + uri + "?" + toQueryString(params)).post(body);
            } else {
                builder = new Request.Builder().url(API_URL + uri + "?" + toQueryString(params)).get();
            }
            Request request = builder.build();
            Response response = OkHttpClientUtil.client.newCall(request).execute();
            String s = response.body().string();
            return s;
        } catch (IOException e) {
            log.error("调用 Huobi API 调用方法 IO 错误 {} url {} ERROR {} , {}", method, uri, e, e.getMessage());
            throw new RuntimeException("调用 Huobi API 调用方法 IO 错误," + e.getMessage());
        } catch (Exception e) {
            log.error("调用 Huobi API 调用方法 错误 {} url {} ERROR {} , {}", method, uri, e, e.getMessage());
            throw new RuntimeException("调用 Huobi API 调用方法 IO 错误," + e.getMessage());
        }
    }

    /**
     * Encode as "a=1&b=%20&c=&d=AAA"
     *
     * @param params
     * @return
     */
    private String toQueryString(Map<String, String> params) {
        return String.join("&", params.entrySet().stream().filter(v -> v.getValue() != null).map((entry) -> {
            return entry.getKey() + "=" + HuobiSignature.urlEncode(entry.getValue());
        }).collect(Collectors.toList()));
    }


}
