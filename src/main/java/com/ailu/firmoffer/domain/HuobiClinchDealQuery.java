package com.ailu.firmoffer.domain;

import lombok.Data;

/**
 * @Description: 查询当前成交、历史成交
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/21 15:09
 */
@Data
public class HuobiClinchDealQuery {

    private String symbol;
    private String types;
    private String startDate;

}
