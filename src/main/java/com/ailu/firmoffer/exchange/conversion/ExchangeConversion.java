package com.ailu.firmoffer.exchange.conversion;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.dao.bean.*;
import com.ailu.firmoffer.util.SendKafkaUtils;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 交易所信息提供接口
 */
@Slf4j
@Data
@Component
public class ExchangeConversion {


    @Resource
    SendKafkaUtils sendKafkaUtils;

    int kafkaMaxSize = 100;

    /**
     * 密钥信息
     * key:userId
     */
    Map<Long, FirmOfferKey> offerKeys = new ConcurrentHashMap<>();

    /**
     * 用户账户Map
     * key:userId
     * key2:账户类型
     */
    Map<Long, Map<String, List<FirmOfferExchangeBalance>>> blancesMap = new ConcurrentHashMap<>();

    /**
     * 用户持仓Map
     * key:userId
     * key2:持仓币种
     */
    Map<Long, Map<String, List<FirmOfferPosition>>> positionsMap = new ConcurrentHashMap<>();

    /**
     * 用户期货订单Map
     * key:userId
     */
    Map<Long, List<FirmOfferOrderHist>> ordersMap = new ConcurrentHashMap<>();

    /**
     * 用户现货订单 Map
     *
     * @param userId
     * @return
     */
    Map<Long, List<FirmOfferMatchHist>> matchsMap = new ConcurrentHashMap<>();

    /**
     * 用户充值Map
     * key:userId
     */
    Map<Long, List<FirmOfferDepositHist>> depositHists = new ConcurrentHashMap<>();

    /**
     * 用户转账Map
     * key:userId
     */
    Map<Long, List<FirmOfferTransfer>> transfers = new ConcurrentHashMap<>();

    /**
     * 用户转账Map
     * key:userId
     */
    Map<Long, List<FirmOfferLedgerHist>> ledgers = new ConcurrentHashMap<>();


    public FirmOfferKey getOfferKeys(Long userId) {
        return offerKeys.get(userId);
    }

    public List<FirmOfferExchangeBalance> getBlancesMap(Long userId,String type) {
        Map<String, List<FirmOfferExchangeBalance>> stringListMap = blancesMap.get(userId);
        if (stringListMap==null){
            return Collections.EMPTY_LIST;
        }
        return stringListMap.getOrDefault(type,Collections.EMPTY_LIST);
    }


    public void addBlanceToMap(Long userId, List<FirmOfferExchangeBalance> list,String type) {
        Map<String, List<FirmOfferExchangeBalance>> map = new HashMap<>();
        map.put(type,list);
        if (blancesMap.get(userId)==null){
            blancesMap.put(userId, map);
        }
        blancesMap.get(userId).put(type,list);
    }

    public List<FirmOfferPosition> getPositionsMap(Long userId,String orderType) {
        Map<String, List<FirmOfferPosition>> stringListMap = positionsMap.get(userId);
        if (stringListMap==null){
            return Collections.EMPTY_LIST;
        }
        return stringListMap.getOrDefault(orderType,Collections.EMPTY_LIST);
    }

    public void addPositionToMap(Long userId,String orderType, List<FirmOfferPosition> list) {
        Map<String, List<FirmOfferPosition>> map = new HashMap<>();
        map.put(orderType,list);
        if (positionsMap.get(userId)==null){
            positionsMap.put(userId, map);
        }
        positionsMap.get(userId).put(orderType,list);
    }

    public List<FirmOfferOrderHist> getOrdersMap(Long userId) {
        return ordersMap.getOrDefault(userId, Collections.EMPTY_LIST);
    }

    public void addDepositToMap(Long userId, List<FirmOfferDepositHist> list) {
        depositHists.put(userId, list);
    }

    public void addTransfersToMap(Long userId, List<FirmOfferTransfer> list) {
        transfers.put(userId, list);
    }

    public void addOrdersToMap(Long userId, List<FirmOfferOrderHist> list) {
        ordersMap.put(userId, list);
    }

    public List<FirmOfferMatchHist> getMatchsMap(Long userId) {
        return matchsMap.getOrDefault(userId, Collections.EMPTY_LIST);
    }

    public void addMatchsToMap(Long userId, List<FirmOfferMatchHist> list) {
        matchsMap.put(userId, list);
    }

    void replaceBlance(Long userId, List<FirmOfferExchangeBalance> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.BLANCE).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        addBlanceToMap(userId,list,orderType);
        List<List<?>> lists = sendKafkaTool(list, userId,exchange,orderType,operationType);
        for (List<?> li:lists) {
            sendKafkaUtils.sendFirmOffer(exchange, key.toString(), JSON.toJSONString(li));
        }
    }

    void replaceMatch(Long userId, List<FirmOfferMatchHist> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.MATCH).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        batchSend(userId, list, exchange, orderType, operationType, key);
        addMatchsToMap(userId, list);
    }

    private void batchSend(Long userId, List<?> list, String exchange, String orderType, String operationType, StringBuilder key) {
        List<List<?>> lists = sendKafkaTool(list, userId,exchange,orderType,operationType);
        for (List<?> li:lists) {
            sendKafkaUtils.sendFirmOffer(exchange, key.toString(), JSON.toJSONString(li));
        }
    }

    void replaceOrder(Long userId, List<FirmOfferOrderHist> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.ORDER).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        List<FirmOfferOrderHist> sendList = new ArrayList<>();
        for (FirmOfferOrderHist firmOfferOrderHist : list) {
            if (firmOfferOrderHist.getPrice() == null) {
                continue;
            }
            sendList.add(firmOfferOrderHist);
        }
        batchSend(userId, list, exchange, orderType, operationType, key);
        addOrdersToMap(userId, list);
    }

    void replacePosition(Long userId, List<FirmOfferPosition> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.POSITION).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        batchSend(userId, list, exchange, orderType, operationType, key);
        addPositionToMap(userId, orderType,list);
    }

    void replaceDeposit(Long userId, List<FirmOfferDepositHist> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.DEPOSIT).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        batchSend(userId, list, exchange, orderType, operationType, key);
        addDepositToMap(userId, list);
    }

    void replaceTransfer(Long userId, List<FirmOfferTransfer> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.TRANSFER).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        batchSend(userId, list, exchange, orderType, operationType, key);
        addTransfersToMap(userId, list);
    }

    void replaceLedger(Long userId, List<FirmOfferLedgerHist> list, String exchange, String orderType, String operationType) {
        StringBuilder key = new StringBuilder(exchange);
        key.append(Dic.SEPARATOR).append(Dic.LEDGER).append(Dic.SEPARATOR).append(orderType).append(Dic.SEPARATOR).append(operationType).append(Dic.SEPARATOR).append(userId);
        batchSend(userId, list, exchange, orderType, operationType, key);
        addLedgerToMap(userId, list);
    }

    void addLedgerToMap(Long userId, List<FirmOfferLedgerHist> list) {
        ledgers.put(userId, list);
    }

    List<List<?>> sendKafkaTool(List<?> list,Long userId,String exchange, String orderType, String operationType){
        List<List<?>> lists = new ArrayList<>();
        if (list.size() > kafkaMaxSize) {
            int i1 = list.size() / kafkaMaxSize;
            int skipNum;
            int limitNum = kafkaMaxSize;
            log.info("本次推出 {} 用户{} {} {} 总条数：{} ",exchange,orderType,userId,operationType, list.size());
            for (int i = 1; i <= i1 + 1; i++) {
                skipNum = (i - 1) * limitNum;
                List<?> collect = list.stream().skip(skipNum).limit(limitNum).collect(Collectors.toList());
                lists.add(collect);
                log.debug("本次实际推出用户{} {} {} {} 条数：{}",userId,exchange,orderType,operationType, collect.size());
            }
        } else {
            lists.add(list);
        }
        return lists;
    }

    /**
     * 获取需要更新的期货订单信息
     * 1.首先查看内存中的老数据是否为 null或空集合，如果没有则认为是第一次，全部抓取
     * 2.将内存中的数据根据 orderId与新数据进行比较判断是否需要更新其订单信息
     * 3.返回需要更新的期货订单信息
     *
     * @param list
     * @return
     */
    public List<FirmOfferOrderHist> getNeedUpdateOrderHists(List<FirmOfferOrderHist> list, Long userId) {
        List<FirmOfferOrderHist> oldOrders = getOrdersMap(userId);
        if (oldOrders.size() == 0) {
            //如果内存中的订单信息为 null，则返回全部数据
            return list;
        }
        Map<String, FirmOfferOrderHist> oldOrderMap = oldOrders.stream().collect(Collectors.toMap(FirmOfferOrderHist::getId, Function.identity()));
        return list.stream().filter(firmOfferOrderHist -> oldOrderMap.get(firmOfferOrderHist.getId()) == null
                || firmOfferOrderHist.getFieldAmount() == null
                || oldOrderMap.get(firmOfferOrderHist.getId()).getFieldAmount() == null
                || firmOfferOrderHist.getFieldAmount().compareTo(oldOrderMap.get(firmOfferOrderHist.getId()).getFieldAmount()) != 0).collect(Collectors.toList());
    }

}
