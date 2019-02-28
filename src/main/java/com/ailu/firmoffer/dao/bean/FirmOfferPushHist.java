package com.ailu.firmoffer.dao.bean;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FirmOfferPushHist {
    private Integer id;

    private String exchange;

    private BigDecimal price;

    private String type;

    private String symbol;

    private BigDecimal amount;

    private Date time;

    private Long mTime;

    private Date utime;

    private Integer userId;

    private String userName;

    private String orderType;
}