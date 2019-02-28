package com.ailu.firmoffer.dao.mapper.ext;

import com.ailu.firmoffer.domain.CoinContrasts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/5/8 9:44
 */
@Repository("firmOfferCoinContrastsExt")
@Mapper
public interface FirmOfferCoinContrastsExt {

    @Select("SELECT p.id,p.symbol,p.coin,p.chinese,c.symbol childSymbol,c.ex_change exChange " +
            "FROM " +
            "firm_offer_coin_contrasts p LEFT JOIN firm_offer_coin_contrasts c ON p.id = c.parent " +
            "WHERE c.ex_change IS NOT NULL AND p.parent is NOT NULL AND p.coin IS NOT NULL")
    List<CoinContrasts> getCoinContrastsTable();


}
