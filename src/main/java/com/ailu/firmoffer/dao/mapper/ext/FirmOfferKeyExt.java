package com.ailu.firmoffer.dao.mapper.ext;

import com.ailu.firmoffer.vo.UserBalanceKeys;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/23 16:53
 */
@Repository("firmOfferKeyExt")
@Mapper
public interface FirmOfferKeyExt {

    @Update("update firm_offer_key set spider_num = #{spiderNum} where user_id = #{userId}")
    void updateUserSpiderNum(@Param("spiderNum") String spiderNum,@Param("userId") Long userId);

}
