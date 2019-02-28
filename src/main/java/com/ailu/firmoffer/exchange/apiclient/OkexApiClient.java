package com.ailu.firmoffer.exchange.apiclient;

import com.ailu.firmoffer.exchange.signature.OkexSignature;
import com.ailu.firmoffer.util.OkHttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/15 15:19
 */
@Slf4j
public class OkexApiClient extends OkexSignature {

    static final String API_HOST = "https://www.okex.com";

    final String accessKeyId;
    final String accessKeySecret;

    public static final String OKEX_INFO_PARAMETER_DEFAULT = "-1";

    public OkexApiClient(String accessKeyId, String accessKeySecret) {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    /**
     * 调用频率  6次/2秒/350ms
     *
     * @return
     */
    public String getCoinBalance() {
        HashMap map = new HashMap(2);
        return post("/api/v1/userinfo.do?", map);
    }

    /**
     * 调用频率  6次/2秒/350ms
     *
     * @return
     */
    public String getCoinAccounts() {
        HashMap map = new HashMap(2);
        return get("/api/spot/v3/accounts?", map);
    }

    public String getAllFuturesBalance() {
        HashMap map = new HashMap(2);
        return post("/api/v1/future_userinfo.do?", map);
    }

    public String getByFuturesBalance() {
        HashMap map = new HashMap(2);
        return post("/api/v1/future_userinfo_4fix.do?", map);
    }

    public String getFuturesPosition() {
        HashMap map = new HashMap(2);
        return get("/api/futures/v3/position?", map);
    }

    /**
     * 调用频率 20次/2秒/100ms
     *
     * @param symbol
     * @return
     */
    public String getCoinOrderId(String symbol) {
        return getCoinOrderInfo(symbol, OKEX_INFO_PARAMETER_DEFAULT);
    }

    /**
     * 调用频率 20次/2秒/100ms
     *
     * @param symbol
     * @return
     */
    public String getCoinOrderInfo(String symbol, String orderId) {
        HashMap map = new HashMap(6);
        map.put("symbol", symbol);
        map.put("order_id", orderId);
        return post("/api/v1/order_info.do?", map);
    }

    public String getOrderHistory(String symbol, Integer status) {
        HashMap map = new HashMap(6);
        //status  1 查询已完成的订单  2 未完成的订单
        map.put("status", String.valueOf(status));
        map.put("symbol", symbol);
        map.put("current_page", String.valueOf(1));
        map.put("page_length", String.valueOf(20));
        return post("/api/v1/order_history.do?", map);
    }

    public String getfutureOrderHistory(String symbol, String contractType, Integer status) {
        HashMap map = new HashMap(6);
        //status  查询状态 1:未完成的订单 2:已经完成的订单
        map.put("status", String.valueOf(status));
        //订单ID -1:查询指定状态的订单，否则查询相应订单号的订单
        map.put("order_id", "-1");
        map.put("symbol", symbol);
        map.put("contract_type", contractType);
        map.put("current_page", String.valueOf(1));
        map.put("page_length", String.valueOf(50));
        return post("/api/v1/future_order_info.do?", map);
    }


    /**
     * send a POST request
     *
     * @param uri
     * @param params
     * @return
     */
    private String post(String uri, Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }
        return call("POST", uri, params);
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
        return call("GET", uri, params);
    }


    /**
     * call api by endpoint
     *
     * @param method
     * @param uri
     * @param params
     * @return
     */
    private String call(String method, String uri, Map<String, String> params) {
        log.trace("准备开始调用 Okex API 调用方法 {} url {}", method, uri);
        params.put("api_key", this.accessKeyId);
        String sign = getSignature(params, this.accessKeySecret);
        params.put("sign", sign);
        try {
            Request.Builder builder;
            if (OkHttpClientUtil.REQUEST_POST.equals(method)) {

                FormBody.Builder rqeBuilder = new FormBody.Builder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    rqeBuilder.add(entry.getKey(), entry.getValue());
                }
                RequestBody body = rqeBuilder.build();
                builder = new Request.Builder().url(API_HOST + uri).post(body);
            } else {
                builder = new Request.Builder().url(API_HOST + uri).get();
            }
            Request request = builder.build();
            Response response = OkHttpClientUtil.client.newCall(request).execute();
            String s = response.body().string();
            return s;
        } catch (IOException e) {
            log.error("调用 Okex API 调用方法 IO 错误 {} url {} ERROR {} , {}", method, uri, e, e.getMessage());
            throw new RuntimeException("调用 Okex API 调用方法 IO 错误," + e.getMessage());
        } catch (Exception e) {
            log.error("调用 Okex API 调用方法 错误 {} url {} ERROR {} , {}", method, uri, e, e.getMessage());
            throw new RuntimeException("调用 Okex API 调用方法 错误," + e.getMessage());
        }
    }

}
