package com.ailu.firmoffer.domain;

import lombok.Data;

import java.util.List;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/23 13:10
 */
@Data
public class HuobiResponseApi<T> {
    private String status;
    private String errCode;
    private String errMsg;
    private List<T> data;
}
