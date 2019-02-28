ALTER TABLE `test_blz`.`firm_offer_key`
ADD COLUMN `spider_num` int(10)NULL COMMENT '所属蜘蛛' AFTER `numberOfDays`;