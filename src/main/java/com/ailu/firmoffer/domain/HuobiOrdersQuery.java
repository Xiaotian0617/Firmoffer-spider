package com.ailu.firmoffer.domain;

import lombok.Data;

/**
 * @Description: 查询当前委托 历史委托
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/21 14:23
 */
@Data
public class HuobiOrdersQuery {
    public HuobiOrdersQuery(String symbol) {
        this.symbol = symbol;
    }

    public HuobiOrdersQuery(String symbol, String states) {
        this.symbol = symbol;
        this.states = states;
    }

    public HuobiOrdersQuery() {
    }

    private String symbol;
    private String states;
    private String types;
    private String startDate;
    private String endDate;
    private String from;
    private String direct;
    private String size;
    private String currency;
}
