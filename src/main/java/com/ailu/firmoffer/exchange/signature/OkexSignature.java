package com.ailu.firmoffer.exchange.signature;

import java.util.Map;

import static com.ailu.firmoffer.util.MD5Util.buildMysignV1;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/15 16:03
 */
public class OkexSignature {

    public String getSignature(Map<String, String> params, String apiKeySecret) {
        return buildMysignV1(params, apiKeySecret);
    }

}
