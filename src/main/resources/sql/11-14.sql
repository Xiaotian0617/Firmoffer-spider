ALTER TABLE `blz`.`firm_offer_exchange_balance`
ADD UNIQUE INDEX `unique_idex`(`user_id`, `ex_change`, `symbol`, `type`)USING BTREE;