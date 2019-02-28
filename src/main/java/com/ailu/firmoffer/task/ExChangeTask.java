package com.ailu.firmoffer.task;

import com.ailu.firmoffer.domain.PendingObj;

import java.util.Set;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/15 14:24
 */
public interface ExChangeTask {
    default void refresh(Set<PendingObj> set) {
        synchronized (set) {
            set.clear();
        }
    }

    default void addOne(Set<PendingObj> set, PendingObj pendingObj) {
        synchronized (set) {
            set.add(pendingObj);
        }
    }

    default void subtractOne(Set<PendingObj> set, PendingObj pendingObj) {
        synchronized (set) {
            set.remove(pendingObj);
        }
    }

    void balances();

    void position();

    void ordersHist();

    void matchsHist();

}
