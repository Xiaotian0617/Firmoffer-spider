package com.ailu.firmoffer.exchange.signature;

import com.alibaba.fastjson.JSONObject;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.Map;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/19 9:22
 */
@Slf4j
@Component
public class BitfinexSignature {

//    private static long nonce = System.currentTimeMillis();

    private static final String ALGORITHM_HMACSHA384 = "HmacSHA384";


    public Map iniHeaders(String apiKey, String apiKeySecret, String urlPath, Map params) {
        String payloadBase64 = getPayloadBase64(urlPath, params);

        String payloadSha384hmac = hmacDigest(payloadBase64, apiKeySecret, ALGORITHM_HMACSHA384);

        Map<String, String> headers = new Hashtable<>();
        headers.put("Content-Type", "form-data");
        headers.put("Accept", "application/json");
        headers.put("X-BFX-APIKEY", apiKey);
        headers.put("X-BFX-PAYLOAD", payloadBase64);
        headers.put("X-BFX-SIGNATURE", payloadSha384hmac);
        log.debug("bitfinex: iniHeaders() : " + headers.toString());
        return headers;
    }

    public Map iniHeaders(String apiKey, String apiKeySecret, String urlPath) {
        String payloadBase64 = getPayloadBase64(urlPath);

        String payloadSha384hmac = hmacDigest(payloadBase64, apiKeySecret, ALGORITHM_HMACSHA384);

        Map<String, String> headers = new Hashtable<>();
        headers.put("Content-Type", "form-data");
        headers.put("Accept", "application/json");
        headers.put("X-BFX-APIKEY", apiKey);
        headers.put("X-BFX-PAYLOAD", payloadBase64);
        headers.put("X-BFX-SIGNATURE", payloadSha384hmac);
        log.debug("bitfinex: iniHeaders() : " + headers.toString());
        return headers;
    }

    private String getPayloadBase64(String urlPath, Map params) {
        JSONObject jo = new JSONObject();
        jo.put("request", urlPath);
        jo.put("nonce", Long.toString(getNonce()));
        jo.putAll(params);
        String payload = jo.toString();
        return Base64.encode(payload.getBytes());
    }

    private String getPayloadBase64(String urlPath) {
        JSONObject jo = new JSONObject();
        jo.put("request", urlPath);
        jo.put("nonce", Long.toString(getNonce()));
        String payload = jo.toString();
        return Base64.encode(payload.getBytes());
    }

    public synchronized static long getNonce() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error("getNonce {} {} ", e, e.getMessage());
        }
        long nonce = System.currentTimeMillis();
        return ++nonce;
    }

    public static String hmacDigest(String msg, String apiKeySecret, String algo) {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec((apiKeySecret).getBytes("UTF-8"), algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(key);

            byte[] bytes = mac.doFinal(msg.getBytes("ASCII"));

            StringBuffer hash = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("Exception:" + e);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            log.error("Exception:" + e);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            log.error("Exception:" + e);

        }
        return digest;
    }

}
