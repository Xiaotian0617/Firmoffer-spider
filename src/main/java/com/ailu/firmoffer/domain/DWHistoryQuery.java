package com.ailu.firmoffer.domain;

import lombok.Data;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/20 10:21
 */
@Data
public class DWHistoryQuery {
    private String currency;
    private String method;
    private String since;
    private Long until;
    private int limit;
}
