package com.ailu.firmoffer.dao.mapper.ext;

import com.ailu.firmoffer.vo.UserBalanceKeys;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/23 16:53
 */
@Repository("firmOfferExchangeBalanceExt")
@Mapper
public interface FirmOfferExchangeBalanceExt {

    @Select("select DISTINCT foeb.user_id as userId,foeb.type,foeb.ex_change as exChange,fok.status,fok.spider_num as spiderNum,foeb.symbol,fok.apiKey,fok.apiKeySecret,fok.passphrase " +
            " from firm_offer_key fok LEFT JOIN firm_offer_exchange_balance foeb ON fok.user_id = foeb.user_id " +
            "where foeb.amount > 0 and (foeb.type = 'future' or foeb.type = 'swap' or foeb.type = 'margin') and fok.status = 1 and foeb.ex_change = 'Okex' and fok.spider_num = #{spiderNum}")
    List<UserBalanceKeys> getBalanceByFuture(@Param("spiderNum") String spiderNum);

    @Select("SELECT DISTINCT symbol FROM firm_offer_exchange_balance where type = \"margin\"")
    List<String> getOkexMarginSymbol();

}
