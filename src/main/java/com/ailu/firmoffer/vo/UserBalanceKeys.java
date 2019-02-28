package com.ailu.firmoffer.vo;

import lombok.Data;

/**
 * 用户Balance keys 信息
 * 暂提供给 Okex 单独抓取期货订单定时
 *
 * @author mr.wang
 * @version 1.0.0
 * @date 2019/1/11 15:19
 */
@Data
public class UserBalanceKeys {

    private Long userId;
    private String exChange;
    private Integer status;
    private Integer spiderNum;
    private String symbol;
    private String apiKey;
    private String apiKeySecret;
    private String passphrase;
    private String type;

    private String key;

    public String getKey() {
        return userId+"_"+type;
    }
}
