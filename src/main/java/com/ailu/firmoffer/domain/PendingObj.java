package com.ailu.firmoffer.domain;

import com.ailu.firmoffer.dao.bean.FirmOfferKey;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/26 15:18
 */
@Data
@EqualsAndHashCode(of = {"type","userId", "symbols"})
public class PendingObj {
    public PendingObj() {
    }

    public PendingObj(FirmOfferKey key, Long userId) {
        this.key = key;
        this.userId = userId;
        this.type = "stock";
    }

    public PendingObj(FirmOfferKey key, Long userId, String type) {
        this.key = key;
        this.userId = userId;
        this.type = type;
    }

    public PendingObj(FirmOfferKey key, Long userId, List<String> symbols) {
        this.key = key;
        this.userId = userId;
        this.symbols = new HashSet<>(symbols);
        this.type = "stock";
    }

    public PendingObj(FirmOfferKey key, Long userId, List<String> symbols, String type) {
        this.key = key;
        this.userId = userId;
        this.symbols = new HashSet<>(symbols);
        this.type = type;
    }

    private FirmOfferKey key;
    private Long userId;
    private Set<String> symbols;
    /**
     * 订单类型 stock 现货 future 期货
     */
    private String type;

    public List<String> getSymbols() {
        return new ArrayList<>(symbols);
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = new HashSet<>(symbols);
    }
}
