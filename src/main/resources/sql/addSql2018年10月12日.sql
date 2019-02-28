ALTER TABLE `blz`.`firm_offer_order_hist`
ADD COLUMN `contract_name` varchar(255)NULL COMMENT '合约名称' AFTER `order_date`,
ADD COLUMN `fee` decimal(32, 12)NULL COMMENT '手续费' AFTER `contract_name`,
ADD COLUMN `unit_amount` decimal(32, 12)NULL COMMENT '合约面值' AFTER `fee`,
ADD COLUMN `lever_rate` decimal(32, 12)NULL COMMENT '杠杆' AFTER `unit_amount`;