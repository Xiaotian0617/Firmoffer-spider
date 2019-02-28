package com.ailu.firmoffer.util;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/21 13:23
 */
@Slf4j
@Component
public class OkHttpClientUtil {

    public static final int CONN_TIMEOUT = 20;
    public static final int READ_TIMEOUT = 20;
    public static final int WRITE_TIMEOUT = 20;

    public static final String REQUEST_POST = "POST";
    public static OkHttpClient client;
    public static final MediaType JSON = MediaType.parse("application/json");
    public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    @Value("${spring.proxy.enable}")
    public boolean proxyEnable;

    @Value("${spring.proxy.url}")
    public String url;
    @Value("${spring.proxy.port}")
    public String port;

    @PostConstruct
    void createOkHttpClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(CONN_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS).writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS);
        if (proxyEnable) {
            log.info("Huobi 启动代理模式！");
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(url, Integer.valueOf(port))));
        }
        client = builder.build();
    }


}
