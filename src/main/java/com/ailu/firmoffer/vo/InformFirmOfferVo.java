package com.ailu.firmoffer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class InformFirmOfferVo {

    private Long id;

    private String leaderId;

    private String leaderName;

    private String img;

    private Integer leaderStatus;

    private Integer sort;

    private Date cTime;

    private Date uTime;

    private BigDecimal earnAll;

    private BigDecimal earnWeek;

    private String leaderInfo;

    private String slogen;

    private String exchange;

    private String leaderType;

}