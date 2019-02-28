package com.ailu.firmoffer.config;

import com.ailu.firmoffer.service.MetaService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/19 11:05
 */
@Data
@Slf4j
//@Component
public class ActivaProperties {

    @Value("${enable.meta.order}")
    public Integer limitLevel;

    public static boolean iniApiKey;
    public static boolean iniCoinContrasts;
    public static boolean intExchangeSymbol;
    public static boolean task;

    @Order(0)
    @EventListener(ApplicationReadyEvent.class)
    public void iniActivation() {
        try {
            iniApiKey = limitLevel >= MetaService.class.getMethod("iniApiKey").getAnnotation(Order.class).value();
            iniCoinContrasts = limitLevel >= MetaService.class.getMethod("iniCoinContrasts").getAnnotation(Order.class).value();
            intExchangeSymbol = limitLevel >= MetaService.class.getMethod("intExchangeSymbol").getAnnotation(Order.class).value();
            task = limitLevel >= MetaService.class.getMethod("task").getAnnotation(Order.class).value();
        } catch (NoSuchMethodException e) {
            log.error("激活初始化功能出错 {} {}", e, e.getMessage());
        }

    }

}
