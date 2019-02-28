package com.ailu.firmoffer.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

import static com.ailu.firmoffer.service.MetaService.*;

/**
 * file:PushKafkaUtils
 * <p>
 * 发送队列往Kafka推送数据方法
 *
 * @author 11:03  王楷
 * @version 11:03 V1.0
 * @par 版权信息：
 * 2018 Copyright 河南艾鹿网络科技有限公司 All Rights Reserved.
 */
@Slf4j
@Component
public class SendKafkaUtils {

    @Resource
    private KafkaTemplate kafkaTemplate;

    @Value("${firmOffer.topic.okex}")
    private String okexTopic;

    @Value("${firmOffer.topic.bitmex}")
    private String bitmexTopic;

    @Value("${firmOffer.topic.huobi}")
    private String huobiTopic;

    @Value("${firmOffer.topic.binance}")
    private String binanceTopic;

    //@Value("${firmOffer.topic.bitfinex}")
    private String bitfinexTopic;

    public void sendFirmOffer(String exchange, String key, String event) {
        switch (exchange) {
            case EX_CHANGE_OKEX:
                sendOkexFirmOffer(key, event);
                break;
            case EX_CHANGE_HUOBI:
                sendHuobiFirmOffer(key, event);
                break;
            case EX_CHANGE_BINANCE:
                sendBinanceFirmOffer(key, event);
                break;
            case EX_CHANGE_BITMEX:
                sendBitmexFirmOffer(key, event);
                break;
            case EX_CHANGE_BITFINEX:
                sendBitmexFirmOffer(key, event);
                break;
            default:
                break;
        }
    }

    private void sendBitmexFirmOffer(String key, String event) {
        kafkaTemplate.send(bitmexTopic, key.toUpperCase(), event);
    }

    public void sendOkexFirmOffer(String key, String event) {
        kafkaTemplate.send(okexTopic, key.toUpperCase(), event);
    }

    public void sendBitfinexFirmOffer(String key, String event) {
        kafkaTemplate.send(bitfinexTopic, key.toUpperCase(), event);
    }

    public void sendHuobiFirmOffer(String key, String event) {
        kafkaTemplate.send(huobiTopic, key, event);
    }

    public void sendBinanceFirmOffer(String key, String event) {
        kafkaTemplate.send(binanceTopic, key, event);
    }

}
