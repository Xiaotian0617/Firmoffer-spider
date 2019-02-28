ALTER TABLE `blz`.`firm_offer_key`
ADD COLUMN `passphrase` varchar(255)NULL COMMENT 'passphrase 仅仅Okex v3有用其他为空' AFTER `status`;