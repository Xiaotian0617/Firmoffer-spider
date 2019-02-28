package com.ailu.firmoffer.dao.bean;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/24 15:12
 */
@Data
public class UserAccountBalance {

    private BigDecimal available;
    private BigDecimal freeze;
    private BigDecimal amount;
    private String symbol;


}
