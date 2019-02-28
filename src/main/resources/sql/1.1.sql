ALTER TABLE `firm_offer_exchange_balance`
ADD COLUMN `loan` decimal(32, 12)NULL COMMENT '借用资产' AFTER `available`;