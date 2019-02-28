package com.ailu.firmoffer.vo;

import lombok.Data;

/**
 * @author mr.wang
 * @version 1.0.0
 * @date 2019/1/22 19:17
 */
@Data
public class Ledger extends com.okcoin.okex.open.api.bean.spot.result.Ledger {

    private String instrument_id;

}
