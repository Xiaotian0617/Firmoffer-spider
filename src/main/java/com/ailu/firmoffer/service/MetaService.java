package com.ailu.firmoffer.service;

import com.ailu.firmoffer.config.Dic;
import com.ailu.firmoffer.dao.bean.FirmOfferCoinContrast;
import com.ailu.firmoffer.dao.bean.FirmOfferCoinContrastExample;
import com.ailu.firmoffer.dao.bean.FirmOfferKey;
import com.ailu.firmoffer.dao.bean.FirmOfferKeyExample;
import com.ailu.firmoffer.dao.mapper.FirmOfferCoinContrastMapper;
import com.ailu.firmoffer.dao.mapper.FirmOfferKeyMapper;
import com.ailu.firmoffer.dao.mapper.ext.FirmOfferExchangeBalanceExt;
import com.ailu.firmoffer.domain.HuobiSymbolResponse;
import com.ailu.firmoffer.domain.PendingObj;
import com.ailu.firmoffer.exchange.conversion.*;
import com.ailu.firmoffer.manager.CoinContrastManager;
import com.ailu.firmoffer.task.ExChangeTask;
import com.ailu.firmoffer.task.OkexV3Task;
import com.ailu.firmoffer.util.OkHttpClientUtil;
import com.ailu.firmoffer.util.OperationFileUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.ailu.firmoffer.task.Pending.huobiMatchHist;
import static com.ailu.firmoffer.task.Pending.huobiPendingObjMap;

/**
 * @Description:
 * @author: liu zhenming
 * @version: V1.0
 * @date: 2018/4/22 11:59
 */
@Slf4j
@Service
public class MetaService {

    @Resource
    List<ExChangeTask> exChangeTasks;

    @Resource
    ExchangeConversion exchangeConversion;

    @Resource
    OkexV3Conversion okexV3Conversion;

    @Resource
    HuobiConversion huobiConversion;

    @Resource
    BinanceConversion binanceConversion;

    @Resource
    BitmexConversion bitmexConversion;

    @Resource
    FirmOfferKeyMapper keyMapper;

    @Resource
    FirmOfferCoinContrastMapper contrastMapper;

    @Resource
    CoinContrastManager coinContrastManager;

    @Resource
    FirmOfferExchangeBalanceExt firmOfferExchangeBalanceExt;

    String defautSymbol = "[{\"base-currency\":\"btc\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"btcusdt\"},{\"base-currency\":\"bch\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"bchusdt\"},{\"base-currency\":\"eth\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"ethusdt\"},{\"base-currency\":\"etc\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"etcusdt\"},{\"base-currency\":\"ltc\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"ltcusdt\"},{\"base-currency\":\"eos\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"eosusdt\"},{\"base-currency\":\"xrp\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"main\",\"symbol\":\"xrpusdt\"},{\"base-currency\":\"omg\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"omgusdt\"},{\"base-currency\":\"dash\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"dashusdt\"},{\"base-currency\":\"zec\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"zecusdt\"},{\"base-currency\":\"ada\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"adausdt\"},{\"base-currency\":\"steem\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"steemusdt\"},{\"base-currency\":\"iota\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"iotausdt\"},{\"base-currency\":\"ocn\",\"quote-currency\":\"usdt\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"ocnusdt\"},{\"base-currency\":\"soc\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"socusdt\"},{\"base-currency\":\"ctxc\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"ctxcusdt\"},{\"base-currency\":\"act\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"actusdt\"},{\"base-currency\":\"btm\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"btmusdt\"},{\"base-currency\":\"bts\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"btsusdt\"},{\"base-currency\":\"ont\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"ontusdt\"},{\"base-currency\":\"iost\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"iostusdt\"},{\"base-currency\":\"ht\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"htusdt\"},{\"base-currency\":\"trx\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"trxusdt\"},{\"base-currency\":\"dta\",\"quote-currency\":\"usdt\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"dtausdt\"},{\"base-currency\":\"neo\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"neousdt\"},{\"base-currency\":\"qtum\",\"quote-currency\":\"usdt\",\"price-precision\":2,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"qtumusdt\"},{\"base-currency\":\"smt\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"smtusdt\"},{\"base-currency\":\"ela\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"elausdt\"},{\"base-currency\":\"ven\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"venusdt\"},{\"base-currency\":\"theta\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"thetausdt\"},{\"base-currency\":\"snt\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"sntusdt\"},{\"base-currency\":\"zil\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"zilusdt\"},{\"base-currency\":\"xem\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"xemusdt\"},{\"base-currency\":\"nas\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"nasusdt\"},{\"base-currency\":\"ruff\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"ruffusdt\"},{\"base-currency\":\"hsr\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"hsrusdt\"},{\"base-currency\":\"let\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"letusdt\"},{\"base-currency\":\"mds\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"mdsusdt\"},{\"base-currency\":\"storj\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"storjusdt\"},{\"base-currency\":\"elf\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"elfusdt\"},{\"base-currency\":\"itc\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"itcusdt\"},{\"base-currency\":\"cvc\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"cvcusdt\"},{\"base-currency\":\"gnt\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"gntusdt\"},{\"base-currency\":\"xmr\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"xmrbtc\"},{\"base-currency\":\"bch\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"bchbtc\"},{\"base-currency\":\"eth\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"ethbtc\"},{\"base-currency\":\"ltc\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"ltcbtc\"},{\"base-currency\":\"etc\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"etcbtc\"},{\"base-currency\":\"eos\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"main\",\"symbol\":\"eosbtc\"},{\"base-currency\":\"omg\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"omgbtc\"},{\"base-currency\":\"xrp\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"main\",\"symbol\":\"xrpbtc\"},{\"base-currency\":\"dash\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"dashbtc\"},{\"base-currency\":\"zec\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"zecbtc\"},{\"base-currency\":\"ada\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"main\",\"symbol\":\"adabtc\"},{\"base-currency\":\"steem\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"main\",\"symbol\":\"steembtc\"},{\"base-currency\":\"iota\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"main\",\"symbol\":\"iotabtc\"},{\"base-currency\":\"poly\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"polybtc\"},{\"base-currency\":\"kan\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"kanbtc\"},{\"base-currency\":\"lba\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"lbabtc\"},{\"base-currency\":\"wan\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"wanbtc\"},{\"base-currency\":\"bft\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"bftbtc\"},{\"base-currency\":\"btm\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"btmbtc\"},{\"base-currency\":\"ont\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"ontbtc\"},{\"base-currency\":\"iost\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"iostbtc\"},{\"base-currency\":\"ht\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"htbtc\"},{\"base-currency\":\"trx\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"trxbtc\"},{\"base-currency\":\"smt\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"smtbtc\"},{\"base-currency\":\"ela\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"elabtc\"},{\"base-currency\":\"wicc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"wiccbtc\"},{\"base-currency\":\"ocn\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ocnbtc\"},{\"base-currency\":\"zla\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"zlabtc\"},{\"base-currency\":\"abt\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"abtbtc\"},{\"base-currency\":\"mtx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"mtxbtc\"},{\"base-currency\":\"nas\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"nasbtc\"},{\"base-currency\":\"ven\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"venbtc\"},{\"base-currency\":\"dta\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dtabtc\"},{\"base-currency\":\"neo\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"neobtc\"},{\"base-currency\":\"wax\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"waxbtc\"},{\"base-currency\":\"bts\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"btsbtc\"},{\"base-currency\":\"zil\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"zilbtc\"},{\"base-currency\":\"theta\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"thetabtc\"},{\"base-currency\":\"ctxc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ctxcbtc\"},{\"base-currency\":\"srn\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"srnbtc\"},{\"base-currency\":\"xem\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"xembtc\"},{\"base-currency\":\"icx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"icxbtc\"},{\"base-currency\":\"dgd\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"dgdbtc\"},{\"base-currency\":\"chat\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"chatbtc\"},{\"base-currency\":\"wpr\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"wprbtc\"},{\"base-currency\":\"lun\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"lunbtc\"},{\"base-currency\":\"swftc\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"swftcbtc\"},{\"base-currency\":\"snt\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"sntbtc\"},{\"base-currency\":\"meet\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"meetbtc\"},{\"base-currency\":\"yee\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"yeebtc\"},{\"base-currency\":\"elf\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"elfbtc\"},{\"base-currency\":\"let\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"letbtc\"},{\"base-currency\":\"qtum\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"qtumbtc\"},{\"base-currency\":\"lsk\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"lskbtc\"},{\"base-currency\":\"itc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"itcbtc\"},{\"base-currency\":\"soc\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"socbtc\"},{\"base-currency\":\"qash\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"qashbtc\"},{\"base-currency\":\"mds\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"mdsbtc\"},{\"base-currency\":\"eko\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ekobtc\"},{\"base-currency\":\"topc\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"topcbtc\"},{\"base-currency\":\"mtn\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"mtnbtc\"},{\"base-currency\":\"act\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"actbtc\"},{\"base-currency\":\"hsr\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"hsrbtc\"},{\"base-currency\":\"stk\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"stkbtc\"},{\"base-currency\":\"storj\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"storjbtc\"},{\"base-currency\":\"gnx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"gnxbtc\"},{\"base-currency\":\"dbc\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dbcbtc\"},{\"base-currency\":\"snc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"sncbtc\"},{\"base-currency\":\"cmt\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"cmtbtc\"},{\"base-currency\":\"tnb\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"tnbbtc\"},{\"base-currency\":\"ruff\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ruffbtc\"},{\"base-currency\":\"qun\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"qunbtc\"},{\"base-currency\":\"zrx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"zrxbtc\"},{\"base-currency\":\"knc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"kncbtc\"},{\"base-currency\":\"blz\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"blzbtc\"},{\"base-currency\":\"propy\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"propybtc\"},{\"base-currency\":\"rpx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"rpxbtc\"},{\"base-currency\":\"appc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"appcbtc\"},{\"base-currency\":\"aidoc\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"aidocbtc\"},{\"base-currency\":\"powr\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"powrbtc\"},{\"base-currency\":\"cvc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"cvcbtc\"},{\"base-currency\":\"pay\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"paybtc\"},{\"base-currency\":\"qsp\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"qspbtc\"},{\"base-currency\":\"dat\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"datbtc\"},{\"base-currency\":\"rdn\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"rdnbtc\"},{\"base-currency\":\"mco\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"mcobtc\"},{\"base-currency\":\"rcn\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"rcnbtc\"},{\"base-currency\":\"mana\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"manabtc\"},{\"base-currency\":\"utk\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"utkbtc\"},{\"base-currency\":\"tnt\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"tntbtc\"},{\"base-currency\":\"gas\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"gasbtc\"},{\"base-currency\":\"bat\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"batbtc\"},{\"base-currency\":\"ost\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ostbtc\"},{\"base-currency\":\"link\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"linkbtc\"},{\"base-currency\":\"gnt\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"gntbtc\"},{\"base-currency\":\"mtl\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"mtlbtc\"},{\"base-currency\":\"evx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"evxbtc\"},{\"base-currency\":\"req\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":1,\"symbol-partition\":\"innovation\",\"symbol\":\"reqbtc\"},{\"base-currency\":\"adx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"adxbtc\"},{\"base-currency\":\"ast\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"astbtc\"},{\"base-currency\":\"eng\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"engbtc\"},{\"base-currency\":\"salt\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"saltbtc\"},{\"base-currency\":\"edu\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"edubtc\"},{\"base-currency\":\"xvg\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"xvgbtc\"},{\"base-currency\":\"wtc\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"wtcbtc\"},{\"base-currency\":\"bifi\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"bifibtc\"},{\"base-currency\":\"bcx\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"bcxbtc\"},{\"base-currency\":\"bcd\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"bcdbtc\"},{\"base-currency\":\"sbtc\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"sbtcbtc\"},{\"base-currency\":\"btg\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"btgbtc\"},{\"base-currency\":\"xmr\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"xmreth\"},{\"base-currency\":\"eos\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"main\",\"symbol\":\"eoseth\"},{\"base-currency\":\"omg\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"omgeth\"},{\"base-currency\":\"iota\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"iotaeth\"},{\"base-currency\":\"ada\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"adaeth\"},{\"base-currency\":\"steem\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"steemeth\"},{\"base-currency\":\"poly\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"polyeth\"},{\"base-currency\":\"kan\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"kaneth\"},{\"base-currency\":\"lba\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"lbaeth\"},{\"base-currency\":\"wan\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"waneth\"},{\"base-currency\":\"bft\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"bfteth\"},{\"base-currency\":\"zrx\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"zrxeth\"},{\"base-currency\":\"ast\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"asteth\"},{\"base-currency\":\"knc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"knceth\"},{\"base-currency\":\"ont\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"onteth\"},{\"base-currency\":\"ht\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"hteth\"},{\"base-currency\":\"btm\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"btmeth\"},{\"base-currency\":\"iost\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"iosteth\"},{\"base-currency\":\"smt\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"smteth\"},{\"base-currency\":\"ela\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"elaeth\"},{\"base-currency\":\"trx\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"trxeth\"},{\"base-currency\":\"abt\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"abteth\"},{\"base-currency\":\"nas\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"naseth\"},{\"base-currency\":\"ocn\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ocneth\"},{\"base-currency\":\"wicc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"wicceth\"},{\"base-currency\":\"zil\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"zileth\"},{\"base-currency\":\"ctxc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ctxceth\"},{\"base-currency\":\"zla\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"zlaeth\"},{\"base-currency\":\"wpr\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"wpreth\"},{\"base-currency\":\"dta\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dtaeth\"},{\"base-currency\":\"mtx\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"mtxeth\"},{\"base-currency\":\"theta\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"thetaeth\"},{\"base-currency\":\"srn\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"srneth\"},{\"base-currency\":\"ven\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"veneth\"},{\"base-currency\":\"bts\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"btseth\"},{\"base-currency\":\"wax\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"waxeth\"},{\"base-currency\":\"hsr\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"hsreth\"},{\"base-currency\":\"icx\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"icxeth\"},{\"base-currency\":\"mtn\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"mtneth\"},{\"base-currency\":\"act\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"acteth\"},{\"base-currency\":\"blz\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"blzeth\"},{\"base-currency\":\"qash\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"qasheth\"},{\"base-currency\":\"ruff\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ruffeth\"},{\"base-currency\":\"cmt\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"cmteth\"},{\"base-currency\":\"elf\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"elfeth\"},{\"base-currency\":\"meet\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"meeteth\"},{\"base-currency\":\"soc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"soceth\"},{\"base-currency\":\"qtum\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"qtumeth\"},{\"base-currency\":\"itc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"itceth\"},{\"base-currency\":\"swftc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"swftceth\"},{\"base-currency\":\"yee\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"yeeeth\"},{\"base-currency\":\"lsk\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"lsketh\"},{\"base-currency\":\"lun\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"luneth\"},{\"base-currency\":\"let\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"leteth\"},{\"base-currency\":\"gnx\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"gnxeth\"},{\"base-currency\":\"chat\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"chateth\"},{\"base-currency\":\"eko\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"ekoeth\"},{\"base-currency\":\"topc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"topceth\"},{\"base-currency\":\"dgd\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"dgdeth\"},{\"base-currency\":\"stk\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"stketh\"},{\"base-currency\":\"mds\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"mdseth\"},{\"base-currency\":\"dbc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dbceth\"},{\"base-currency\":\"snc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"snceth\"},{\"base-currency\":\"pay\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"payeth\"},{\"base-currency\":\"qun\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"quneth\"},{\"base-currency\":\"aidoc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"aidoceth\"},{\"base-currency\":\"tnb\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"tnbeth\"},{\"base-currency\":\"appc\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"appceth\"},{\"base-currency\":\"rdn\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"rdneth\"},{\"base-currency\":\"utk\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"utketh\"},{\"base-currency\":\"powr\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"powreth\"},{\"base-currency\":\"bat\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"bateth\"},{\"base-currency\":\"propy\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"propyeth\"},{\"base-currency\":\"mana\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"manaeth\"},{\"base-currency\":\"req\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":1,\"symbol-partition\":\"innovation\",\"symbol\":\"reqeth\"},{\"base-currency\":\"cvc\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"cvceth\"},{\"base-currency\":\"qsp\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"qspeth\"},{\"base-currency\":\"evx\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"evxeth\"},{\"base-currency\":\"dat\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dateth\"},{\"base-currency\":\"mco\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"mcoeth\"},{\"base-currency\":\"gnt\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"gnteth\"},{\"base-currency\":\"gas\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"gaseth\"},{\"base-currency\":\"ost\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"osteth\"},{\"base-currency\":\"link\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"linketh\"},{\"base-currency\":\"rcn\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"rcneth\"},{\"base-currency\":\"tnt\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":0,\"symbol-partition\":\"innovation\",\"symbol\":\"tnteth\"},{\"base-currency\":\"eng\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"engeth\"},{\"base-currency\":\"salt\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"salteth\"},{\"base-currency\":\"adx\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"adxeth\"},{\"base-currency\":\"edu\",\"quote-currency\":\"eth\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"edueth\"},{\"base-currency\":\"xvg\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"xvgeth\"},{\"base-currency\":\"wtc\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"wtceth\"},{\"base-currency\":\"xrp\",\"quote-currency\":\"ht\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"xrpht\"},{\"base-currency\":\"iost\",\"quote-currency\":\"ht\",\"price-precision\":8,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"iostht\"},{\"base-currency\":\"dash\",\"quote-currency\":\"ht\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"dashht\"},{\"base-currency\":\"wicc\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"wiccusdt\"},{\"base-currency\":\"eos\",\"quote-currency\":\"ht\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"eosht\"},{\"base-currency\":\"bch\",\"quote-currency\":\"ht\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"bchht\"},{\"base-currency\":\"ltc\",\"quote-currency\":\"ht\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"ltcht\"},{\"base-currency\":\"etc\",\"quote-currency\":\"ht\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"etcht\"},{\"base-currency\":\"waves\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"wavesbtc\"},{\"base-currency\":\"waves\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"waveseth\"},{\"base-currency\":\"hb10\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"main\",\"symbol\":\"hb10usdt\"},{\"base-currency\":\"cmt\",\"quote-currency\":\"usdt\",\"price-precision\":4,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"cmtusdt\"},{\"base-currency\":\"dcr\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"dcrbtc\"},{\"base-currency\":\"dcr\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"dcreth\"},{\"base-currency\":\"pai\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"paibtc\"},{\"base-currency\":\"pai\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"paieth\"},{\"base-currency\":\"box\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"boxbtc\"},{\"base-currency\":\"box\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"boxeth\"},{\"base-currency\":\"dgb\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dgbbtc\"},{\"base-currency\":\"dgb\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"dgbeth\"},{\"base-currency\":\"gxs\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"gxsbtc\"},{\"base-currency\":\"gxs\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"gxseth\"},{\"base-currency\":\"xlm\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"xlmbtc\"},{\"base-currency\":\"xlm\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"xlmeth\"},{\"base-currency\":\"bix\",\"quote-currency\":\"btc\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"bixbtc\"},{\"base-currency\":\"bix\",\"quote-currency\":\"eth\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"bixeth\"},{\"base-currency\":\"bix\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"bixusdt\"},{\"base-currency\":\"hit\",\"quote-currency\":\"btc\",\"price-precision\":10,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"hitbtc\"},{\"base-currency\":\"hit\",\"quote-currency\":\"eth\",\"price-precision\":8,\"amount-precision\":2,\"symbol-partition\":\"innovation\",\"symbol\":\"hiteth\"},{\"base-currency\":\"pai\",\"quote-currency\":\"usdt\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"innovation\",\"symbol\":\"paiusdt\"},{\"base-currency\":\"bt1\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"bt1btc\"},{\"base-currency\":\"bt2\",\"quote-currency\":\"btc\",\"price-precision\":6,\"amount-precision\":4,\"symbol-partition\":\"bifurcation\",\"symbol\":\"bt2btc\"}]";

    @Resource
    OperationFileUtils operationFileUtils;

    @Value("${spider.num}")
    private Integer spiderNum;

    /**
     * 币种对照表
     */
    public static Map<Integer, FirmOfferCoinContrast> coinContrastMap = new ConcurrentHashMap<>();

    final List<HuobiSymbolResponse> huobiSymbolList = new ArrayList<>();

    public static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    public static final String EX_CHANGE_BITFINEX = "Bitfinex";
    public static final String EX_CHANGE_HUOBI = "Huobi";
    public static final String EX_CHANGE_BIBOX = "Bibox";
    public static final String EX_CHANGE_BINANCE = "Binance";
    public static final String EX_CHANGE_OKEX = "Okex";
    public static final String EX_CHANGE_BITMEX = "Bitmex";

    public Map<Long, FirmOfferKey> getHuobiKey() {
        return huobiConversion.getOfferKeys();
    }

    public Map<Long, FirmOfferKey> getOkexKey() {
        return okexV3Conversion.getOfferKeys();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    private void initApiKey() {
        log.info("开始初始化用户 私钥 信息");
        //List<FirmOfferKey> firmOfferKeys = getFileToObject("", "keys.json", FirmOfferKey.class);
        List<FirmOfferKey> temp = getFirmOfferKeys();
        if (temp == null) {
            log.error("本次获取为null，请重试！");
            return;
        }
        List<FirmOfferKey> firmOfferKeys = temp;
        if (firmOfferKeys == null || firmOfferKeys.isEmpty()) {
            log.error("初始化用户私钥信息为 null");
        } else {
            //初始化用户所有api密钥
            exchangeConversion.setOfferKeys(firmOfferKeys.stream().collect(Collectors.toMap(FirmOfferKey::getUserId, Function.identity())));
            //初始化所有huobi密钥
            huobiConversion.setOfferKeys(firmOfferKeys.stream().filter(
                    key -> key.getExChange().equals(EX_CHANGE_HUOBI) && key.getStatus() == Dic.EX_CHANGE_API_SECRET_KEY_STATUS_NORMAL
            ).collect(Collectors.toMap(FirmOfferKey::getUserId, Function.identity())));
            okexV3Conversion.setOfferKeys(firmOfferKeys.stream().filter(
                    key -> key.getExChange().equals(EX_CHANGE_OKEX) && key.getStatus() == Dic.EX_CHANGE_API_SECRET_KEY_STATUS_NORMAL
            ).collect(Collectors.toMap(FirmOfferKey::getUserId, Function.identity())));

            binanceConversion.setOfferKeys(firmOfferKeys.stream().filter(
                    key -> key.getExChange().equals(EX_CHANGE_BINANCE) && key.getStatus() == Dic.EX_CHANGE_API_SECRET_KEY_STATUS_NORMAL
            ).collect(Collectors.toMap(FirmOfferKey::getUserId, Function.identity())));

            bitmexConversion.setOfferKeys(firmOfferKeys.stream().filter(
                    key -> key.getExChange().equals(EX_CHANGE_BITMEX) && key.getStatus() == Dic.EX_CHANGE_API_SECRET_KEY_STATUS_NORMAL
            ).collect(Collectors.toMap(FirmOfferKey::getUserId, Function.identity())));
            log.info("初始化用户私钥信息成功 ： 共初始化用户 {} 个，私钥总数量 {} 个，Huobi 密钥 {} 个,Okex 密钥 {} 个,Bitmex 秘钥 {} 个,Binance 密钥 {} 个",
                    exchangeConversion.getOfferKeys().size(), firmOfferKeys.size(), huobiConversion.getOfferKeys().size(), okexV3Conversion.getOfferKeys().size(), bitmexConversion.getOfferKeys().size(), binanceConversion.getOfferKeys().size());
        }

    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    private void iniCoinContrast() {
        log.info("开始初始化币种对照表信息");
        //List<FirmOfferCoinContrast> contrastList = getFileToObject("", "contrast.json", FirmOfferCoinContrast.class);
        List<FirmOfferCoinContrast> contrastList = gerCoinContrast();
        if (contrastList == null || contrastList.isEmpty()) {
            log.error("初始化币种对照表信息为 null");
        } else {
            coinContrastMap = contrastList.stream().collect(Collectors.toMap(FirmOfferCoinContrast::getId, Function.identity()));
            log.info("初始化币种对照表信息 成功 ：共初始化币种数量 {} 个", coinContrastMap.size());
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    private void intHuobiSymbol() {
        log.info("开始初始化huobi 所有Symbol ");
        try {
            huobiSymbolList.addAll(getHuobiSymbol());
        } catch (Throwable e) {
            log.error("获取火币symbol失败，启用默认币种！");
            huobiSymbolList.addAll(JSON.parseArray(defautSymbol, HuobiSymbolResponse.class));
        }
        if (huobiSymbolList != null || huobiSymbolList.size() > 1) {
            huobiConversion.setHuobiSymbol(huobiSymbolList.stream().collect(Collectors.groupingBy(HuobiSymbolResponse::getBaseCurrency, Collectors.toList())));
            log.info("初始化huobi Symbol 成功 : 共初始化币种交易对 {} 个", huobiConversion.getHuobiSymbolSize());
            huobiConversion.getOfferKeys().values().forEach(firmOfferKey -> {
                huobiMatchHist.add(new PendingObj(firmOfferKey, firmOfferKey.getUserId(),
                        huobiSymbolList.stream().map(HuobiSymbolResponse::getBaseCurrency)
                                .collect(Collectors.toList())));
            });
            huobiPendingObjMap = huobiMatchHist.stream().collect(Collectors.toMap(PendingObj::getUserId, Function.identity()));
        } else {
            log.error("初始化huobi Symbol 失败");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(4)
    private void task() {
        coinContrastManager.init();
        exChangeTasks.forEach(exChangeTask -> {
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    initApiKey();
                } catch (Throwable e) {
                    log.error("更新Appkey出错", e);
                }
            }, 1, 1, TimeUnit.MINUTES);
            String exchangeName = exChangeTask.getClass().getName();
            scheduledExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    log.debug("开始获取{}持仓信息", exchangeName);
                    exChangeTask.balances();
                    log.debug("获取{}持仓信息结束", exchangeName);
                } catch (Throwable e) {
                    log.error("获取" + exchangeName + "持仓信息出错！", e);
                }
            }, 10, 1000, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    log.debug("开始获取{}期货订单列表信息", exchangeName);
                    exChangeTask.ordersHist();
                    log.debug("获取{}订单期货列表信息结束", exchangeName);
                } catch (Throwable e) {
                    log.error("获取" + exchangeName + "订单期货列表信息出错！", e);
                }
            }, 10, 1000, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                try {
                    log.debug("开始获取{}现货订单列表信息", exchangeName);
                    exChangeTask.matchsHist();
                    log.debug("获取{}订单现货列表信息结束", exchangeName);
                } catch (Throwable e) {
                    log.error("获取" + exchangeName + "订单现货列表信息出错！", e);
                }
            }, 10, 1000, TimeUnit.MILLISECONDS);
            scheduledExecutorService.scheduleWithFixedDelay(() -> {
                try {
                    log.debug("开始获取{}持仓列表信息", exchangeName);
                    exChangeTask.position();
                    log.debug("获取{}持仓列表信息结束", exchangeName);
                } catch (Throwable e) {
                    log.error("获取" + exchangeName + "持仓列表信息出错！", e);
                }
            }, 10, 1000, TimeUnit.MILLISECONDS);
        });
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                log.debug("开始更新Okex币币杠杆币种");
                updateOkexMarginSymbol();
                log.debug("更新Okex币币杠杆币种结束");
            } catch (Throwable e) {
                log.error("更新Okex币币杠杆币种出错！", e);
            }
        }, 0, 12, TimeUnit.HOURS);
    }

    private void updateOkexMarginSymbol() {
        List<String> okexMarginSymbol = firmOfferExchangeBalanceExt.getOkexMarginSymbol();
        if (okexMarginSymbol==null||okexMarginSymbol.isEmpty()){
            log.warn("get okex margin symbols is Empty");
            return;
        }
        OkexV3Task.addOkexMarginSymbol(okexMarginSymbol);
    }

    public List<HuobiSymbolResponse> getHuobiSymbol() throws IOException {
        Request.Builder builder = new Request.Builder();
        Request request = builder.url("https://api.huobipro.com/v1/common/symbols").build();
        String pro = OkHttpClientUtil.client.newCall(request).execute().body().string();
        request = builder.url("https://api.hadax.com/v1/hadax/common/symbols").build();
        String hadax = OkHttpClientUtil.client.newCall(request).execute().body().string();
        JSONObject proJson = JSON.parseObject(pro, JSONObject.class);
        JSONObject hadaxJson = JSON.parseObject(hadax, JSONObject.class);
        List<HuobiSymbolResponse> prolist = proJson.getJSONArray("data").toJavaList(HuobiSymbolResponse.class);
        List<HuobiSymbolResponse> hadaxlist = hadaxJson.getJSONArray("data").toJavaList(HuobiSymbolResponse.class);
        boolean flag = prolist.addAll(hadaxlist);
        if (!flag) {
            return null;
        }
        return prolist;
    }

    public <T> List<T> getFileToObject(String directoryName, String fileName, Class clazz) {
        String content = operationFileUtils.readFile(directoryName, fileName);
        if (!StringUtils.hasText(content)) {
            log.error("读取文件信息出错！");
            return Collections.EMPTY_LIST;
        }
        JSONObject keys = JSON.parseObject(content);
        List<T> objects = keys.getJSONArray("RECORDS").toJavaList(clazz);
        return objects;
    }

    public List<FirmOfferKey> getFirmOfferKeys() {
        FirmOfferKeyExample firmOfferKeyExample = new FirmOfferKeyExample();
        firmOfferKeyExample.or().andSpiderNumEqualTo(spiderNum);
        return keyMapper.selectByExample(firmOfferKeyExample);
    }

    public List<FirmOfferCoinContrast> gerCoinContrast() {
        FirmOfferCoinContrastExample contrastExample = new FirmOfferCoinContrastExample();
        return contrastMapper.selectByExample(contrastExample);
    }

}
