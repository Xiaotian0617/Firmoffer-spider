CREATE TABLE `firm_offer_key` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`user_id` bigint(20)NOT NULL COMMENT '用户id',
`ex_change` char(50)NOT NULL COMMENT '交易所',
`apiKey` varchar(64)NOT NULL COMMENT 'api key',
`apiKeySecret` varchar(64)NOT NULL COMMENT 'api key Secret',
`status` int(11)NOT NULL COMMENT '密钥状态 （ 1：有效  2: 无效  ）',
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
;

CREATE TABLE `firm_offer_exchange_balance` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`user_id` bigint(20)NOT NULL COMMENT '用户id',
`ex_change` char(50)NOT NULL COMMENT '交易所',
`available` decimal(32, 12)NOT NULL COMMENT '可用金额',
`freeze` decimal(32, 12)NOT NULL COMMENT '冻结金额',
`coin` int(11)DEFAULT NULL COMMENT '币种id',
`symbol` varchar(10)NOT NULL COMMENT '币种',
`amount` decimal(32, 12)NOT NULL COMMENT '总量',
`type` char(10)DEFAULT NULL COMMENT '种类',
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
;

CREATE TABLE `firm_offer_coin_contrast` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
`symbol` char(10)NOT NULL COMMENT '英文简称',
`full` varchar(30)DEFAULT NULL COMMENT '英文全称',
`chinese` varchar(30)DEFAULT NULL COMMENT '中文',
`bitfinex_chinese` varchar(30)DEFAULT NULL COMMENT 'bitfinex中文',
`bitfinex_symbol` char(10)DEFAULT NULL COMMENT 'bitfinex简称',
`bitfinex_full` varchar(30)DEFAULT NULL COMMENT 'bitfinex 全称',
`huobi_full` varchar(30)DEFAULT NULL COMMENT '火币英文全称',
`huobi_symbol` char(10)DEFAULT NULL COMMENT '货币简称',
`huobi_chinese` varchar(30)DEFAULT NULL COMMENT '货币中文',
`coin` int(10)DEFAULT NULL COMMENT '币种id',
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
;


CREATE TABLE `firm_offer_order_hist` (
  `id` bigint(20) NOT NULL COMMENT '主键 订单id',
`user_id` bigint(20)NOT NULL COMMENT '用户id',
`order_status` int(11)NOT NULL COMMENT '订单状态',
`ex_change` varchar(30)NOT NULL COMMENT '交易所',
`trading_on` varchar(40)NOT NULL COMMENT '交易对',
`type` varchar(40)NOT NULL COMMENT '订单类型',
`price` decimal(32, 12)NOT NULL COMMENT '挂单价格',
`side` varchar(20)NOT NULL COMMENT '买卖类型',
`amount` decimal(32, 12)NOT NULL COMMENT '挂单数量',
`field_amount` decimal(32, 12)DEFAULT NULL COMMENT '已成交数量',
`field_price` decimal(32, 12)DEFAULT NULL COMMENT '已成交单价',
`order_date` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)COMMENT '订单时间',
`utime` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)ON
UPDATE CURRENT_TIMESTAMP (6),
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `firm_offer_movements` (
  `id` int(11) NOT NULL COMMENT '主键',
`type` varchar(20)NOT NULL COMMENT '类型',
`currency` char(10)NOT NULL COMMENT '币种',
`amount` decimal(32, 12)NOT NULL COMMENT '数量',
`address` varchar(255)NOT NULL COMMENT '地址',
`fee` decimal(32, 12)NOT NULL COMMENT '手续费',
`state` varchar(20)NOT NULL COMMENT '状态',
`created_time` datetime(6)NOT NULL COMMENT '创建时间',
`updated_time` datetime(6)NOT NULL COMMENT '更新时间',
`txid` varchar(255)DEFAULT NULL COMMENT '交易hash',
`utime` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
`user_id` bigint(20)NOT NULL COMMENT '用户id',
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
;


CREATE TABLE `firm_offer_exchange_balance_snap` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
`user_id` bigint(20)NOT NULL COMMENT '用户ID',
`total` decimal(32, 12)NOT NULL COMMENT '总资产',
`time_number` bigint(20)DEFAULT NULL COMMENT '如果时间是2018-04-27 20180427',
`ctime` datetime(3)DEFAULT NULL,
`utime` datetime(3)DEFAULT NULL,
PRIMARY KEY(`id`),
UNIQUE KEY `unique_idx`(`user_id`, `time_number`),
KEY `idx_ctime`(`ctime`)USING BTREE
)ENGINE = InnoDB AUTO_INCREMENT = 1384 DEFAULT CHARSET = utf8mb4
;

CREATE TABLE `firm_offer_user_ratio_line` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT ' ',
`user_id` bigint(20)DEFAULT NULL,
`ratio` decimal(30, 16)DEFAULT NULL COMMENT '收益率',
`rate_time` timestamp NULL DEFAULT NULL,
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4
;

CREATE TABLE `firm_offer_position` (
  `id` bigint(20) NOT NULL COMMENT '主键 仓位id ',
`user_id` bigint(20)NOT NULL COMMENT '用户id',
`ex_change` varchar(30)NOT NULL COMMENT '交易所',
`trading_on` varchar(40)NOT NULL COMMENT '交易对',
`type` int(11)NOT NULL COMMENT '做多做空  1：做多 2：做空',
`status` varchar(20)NOT NULL COMMENT '状态',
`base` decimal(32, 16)NOT NULL COMMENT '基价',
`amount` decimal(32, 16)NOT NULL COMMENT '金额',
`pl` decimal(32, 16)DEFAULT NULL COMMENT '损益',
`position_date` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)COMMENT '下单时间',
`utime` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)ON
UPDATE CURRENT_TIMESTAMP (6),
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;


CREATE TABLE `user_init`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
`balance` decimal(30, 6)NULL DEFAULT NULL,
`user_id` bigint(20)NULL DEFAULT NULL,
PRIMARY KEY(`id`)USING BTREE
)ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER
SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact
;

CREATE TABLE `firm_offer_match_hist` (
  `id` bigint(20) NOT NULL COMMENT '主键 订单id ',
`user_id` bigint(20)NOT NULL COMMENT '用户id',
`match_id` bigint(20)NOT NULL COMMENT '撮合ID',
`ex_change` varchar(30)NOT NULL COMMENT '交易所',
`symbol` varchar(40)NOT NULL COMMENT '交易对',
`type` varchar(40)NOT NULL COMMENT '订单类型',
`price` decimal(32, 12)NOT NULL COMMENT '成交价格',
`source` varchar(20)NOT NULL COMMENT '订单来源',
`field_fees` decimal(32, 12)NOT NULL COMMENT '成交手续费',
`field_amount` decimal(32, 12)DEFAULT NULL COMMENT '已成交数量',
`field_price` decimal(32, 12)DEFAULT NULL COMMENT '已成交单价',
`match_date` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)COMMENT '成交时间',
`utime` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)ON
UPDATE CURRENT_TIMESTAMP (6),
PRIMARY KEY(`id`)
)ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
ALTER TABLE `blz`.`firm_offer_match_hist`
ADD COLUMN `order_id` bigint(20)NULL COMMENT '订单id' AFTER `user_id`;


INSERT INTO `user_init`
VALUES (1, 15000.000000, 439);
INSERT INTO `user_init`
VALUES (2, 15000.000000, 440);
INSERT INTO `user_init`
VALUES (3, 15000.000000, 441);
INSERT INTO `user_init`
VALUES (4, 15000.000000, 442);
INSERT INTO `user_init`
VALUES (5, 15000.000000, 443);
INSERT INTO `user_init`
VALUES (6, 15000.000000, 444);
INSERT INTO `user_init`
VALUES (7, 15000.000000, 445);
INSERT INTO `user_init`
VALUES (8, 15000.000000, 446);
INSERT INTO `user_init`
VALUES (9, 15000.000000, 447);
INSERT INTO `user_init`
VALUES (10, 15000.000000, 448);

