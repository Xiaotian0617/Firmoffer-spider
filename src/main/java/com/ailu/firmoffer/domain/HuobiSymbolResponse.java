package com.ailu.firmoffer.domain;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/26 10:58
 */
@Data
public class HuobiSymbolResponse {
    @JSONField(name = "base-currency")
    private String baseCurrency;
    @JSONField(name = "quote-currency")
    private String quoteCurrency;
    @JSONField(name = "price-precision")
    private int pricePrecision;
    @JSONField(name = "amount-precision")
    private int amountPrecision;
    @JSONField(name = "symbol-partition")
    private String symbolPartition;

    public String getSymbol() {
        return getBaseCurrency() + getQuoteCurrency();
    }
}
