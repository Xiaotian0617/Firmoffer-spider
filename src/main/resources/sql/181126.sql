ALTER TABLE `blz`.`firm_offer_exchange_balance_snap`
ADD COLUMN `future` decimal(32, 12)NULL COMMENT '期货资产' AFTER `total`,
ADD COLUMN `wallet` decimal(32, 12)NULL COMMENT '钱包资产' AFTER `future`,
ADD COLUMN `stock` decimal(32, 12)NULL COMMENT '现货资产' AFTER `wallet`;


DROP TABLE IF EXISTS `firm_offer_position`;
CREATE TABLE `firm_offer_position`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 自增',
`user_id` bigint(20)NOT NULL COMMENT '用户id',
`ex_change` varchar(30)CHARACTER
SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易所',
`instrument_id` varchar(40)CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '合约id',
`trading_on` varchar(255)CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易对',
`type` varchar(20)CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '01:多仓 02:空仓',
`margin_mode` varchar(50)CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账户类型 crossed全仓 fixed 逐仓',
`position_date` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)COMMENT '下单时间',
`liquidation_price` decimal(32, 16)NULL DEFAULT NULL COMMENT '（全仓）预估爆仓价',
`qty` decimal(32, 16)NULL DEFAULT NULL COMMENT '多仓数量',
`avail_qty` decimal(32, 16)NULL DEFAULT NULL COMMENT '多仓可平仓数量',
`avg_cost` decimal(32, 16)NULL DEFAULT NULL COMMENT '开仓平均价',
`settlement_price` decimal(32, 16)NULL DEFAULT NULL COMMENT '多仓结算基准价',
`liqui_price` decimal(32, 16)NULL DEFAULT NULL COMMENT '（逐仓）多仓强平价格',
`pnl_ratio` decimal(32, 16)NULL DEFAULT NULL COMMENT '（逐仓）多仓收益率',
`margin` decimal(32, 16)NULL DEFAULT NULL COMMENT '（逐仓）多仓保证金',
`realized_pnl` decimal(32, 16)NULL DEFAULT NULL COMMENT '已实现盈余 浮动盈亏',
`leverage` varchar(20)CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '杠杆倍数',
`utime` datetime(6)NOT NULL DEFAULT CURRENT_TIMESTAMP(6)ON UPDATE CURRENT_TIMESTAMP(6),
PRIMARY KEY(`id`)USING BTREE,
UNIQUE INDEX `uni_index`(`user_id`, `ex_change`, `instrument_id`, `type`, `margin_mode`)USING BTREE
)ENGINE = InnoDB AUTO_INCREMENT = 12637 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;