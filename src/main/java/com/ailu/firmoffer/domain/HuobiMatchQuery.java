package com.ailu.firmoffer.domain;

import lombok.Data;

/**
 * @Description: 查询当前委托 历史委托
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/21 14:23
 */
@Data
public class HuobiMatchQuery {
    public HuobiMatchQuery(String symbol) {
        this.symbol = symbol;
    }

    public HuobiMatchQuery(String symbol, String size) {
        this.symbol = symbol;
        this.size = size;
    }

    public HuobiMatchQuery(String symbol, String startDate, String size) {
        this.symbol = symbol;
        this.startDate = startDate;
        this.size = size;
    }


    public HuobiMatchQuery() {
    }

    private String symbol;
    private String types;
    private String startDate;
    private String endDate;
    private String from;
    private String direct;
    private String size;
}
