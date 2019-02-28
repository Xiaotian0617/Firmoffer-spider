CREATE TABLE `firm_offer_coin_contrasts` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
`symbol` char(10)CHARACTER
SET utf8mb4 NOT NULL COMMENT '英文简称',
`full` varchar(30)CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '英文全称',
`chinese` varchar(30)CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '中文',
`coin` int(10)DEFAULT NULL COMMENT '币种id',
`parent` int(11)DEFAULT NULL COMMENT '父级id  最高级为 0',
`ex_change` varchar(30)DEFAULT NULL COMMENT '交易所',
PRIMARY KEY(`id`)
)ENGINE = InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8;

ALTER TABLE `firm_offer_order_hist` MODIFY COLUMN `id` varchar(40)NOT NULL COMMENT '主键 订单id + 交易所' FIRST;

ALTER TABLE `firm_offer_movements` MODIFY COLUMN `id` varchar(40)NOT NULL COMMENT '主键 充值提现id或区分信息 + 交易所' FIRST;

ALTER TABLE `firm_offer_movements`
ADD COLUMN `ex_change` varchar(30)CHARACTER
SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '交易所' AFTER `user_id`;