package com.ailu.firmoffer.dao.bean;

import java.math.BigDecimal;
import java.util.Date;

public class FirmOfferMovements {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.id
     *
     * @mbg.generated
     */
    private String id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.type
     *
     * @mbg.generated
     */
    private String type;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.currency
     *
     * @mbg.generated
     */
    private String currency;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.amount
     *
     * @mbg.generated
     */
    private BigDecimal amount;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.address
     *
     * @mbg.generated
     */
    private String address;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.fee
     *
     * @mbg.generated
     */
    private BigDecimal fee;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.state
     *
     * @mbg.generated
     */
    private String state;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.created_time
     *
     * @mbg.generated
     */
    private Date createdTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.updated_time
     *
     * @mbg.generated
     */
    private Date updatedTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.txid
     *
     * @mbg.generated
     */
    private String txid;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.utime
     *
     * @mbg.generated
     */
    private Date utime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.user_id
     *
     * @mbg.generated
     */
    private Long userId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column firm_offer_movements.ex_change
     *
     * @mbg.generated
     */
    private String exChange;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.id
     *
     * @return the value of firm_offer_movements.id
     * @mbg.generated
     */
    public String getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.id
     *
     * @param id the value for firm_offer_movements.id
     * @mbg.generated
     */
    public void setId(String id) {
        this.id = id == null ? null : id.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.type
     *
     * @return the value of firm_offer_movements.type
     * @mbg.generated
     */
    public String getType() {
        return type;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.type
     *
     * @param type the value for firm_offer_movements.type
     * @mbg.generated
     */
    public void setType(String type) {
        this.type = type == null ? null : type.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.currency
     *
     * @return the value of firm_offer_movements.currency
     * @mbg.generated
     */
    public String getCurrency() {
        return currency;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.currency
     *
     * @param currency the value for firm_offer_movements.currency
     * @mbg.generated
     */
    public void setCurrency(String currency) {
        this.currency = currency == null ? null : currency.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.amount
     *
     * @return the value of firm_offer_movements.amount
     * @mbg.generated
     */
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.amount
     *
     * @param amount the value for firm_offer_movements.amount
     * @mbg.generated
     */
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.address
     *
     * @return the value of firm_offer_movements.address
     * @mbg.generated
     */
    public String getAddress() {
        return address;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.address
     *
     * @param address the value for firm_offer_movements.address
     * @mbg.generated
     */
    public void setAddress(String address) {
        this.address = address == null ? null : address.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.fee
     *
     * @return the value of firm_offer_movements.fee
     * @mbg.generated
     */
    public BigDecimal getFee() {
        return fee;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.fee
     *
     * @param fee the value for firm_offer_movements.fee
     * @mbg.generated
     */
    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.state
     *
     * @return the value of firm_offer_movements.state
     * @mbg.generated
     */
    public String getState() {
        return state;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.state
     *
     * @param state the value for firm_offer_movements.state
     * @mbg.generated
     */
    public void setState(String state) {
        this.state = state == null ? null : state.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.created_time
     *
     * @return the value of firm_offer_movements.created_time
     * @mbg.generated
     */
    public Date getCreatedTime() {
        return createdTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.created_time
     *
     * @param createdTime the value for firm_offer_movements.created_time
     * @mbg.generated
     */
    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.updated_time
     *
     * @return the value of firm_offer_movements.updated_time
     * @mbg.generated
     */
    public Date getUpdatedTime() {
        return updatedTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.updated_time
     *
     * @param updatedTime the value for firm_offer_movements.updated_time
     * @mbg.generated
     */
    public void setUpdatedTime(Date updatedTime) {
        this.updatedTime = updatedTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.txid
     *
     * @return the value of firm_offer_movements.txid
     * @mbg.generated
     */
    public String getTxid() {
        return txid;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.txid
     *
     * @param txid the value for firm_offer_movements.txid
     * @mbg.generated
     */
    public void setTxid(String txid) {
        this.txid = txid == null ? null : txid.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.utime
     *
     * @return the value of firm_offer_movements.utime
     * @mbg.generated
     */
    public Date getUtime() {
        return utime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.utime
     *
     * @param utime the value for firm_offer_movements.utime
     * @mbg.generated
     */
    public void setUtime(Date utime) {
        this.utime = utime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.user_id
     *
     * @return the value of firm_offer_movements.user_id
     * @mbg.generated
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.user_id
     *
     * @param userId the value for firm_offer_movements.user_id
     * @mbg.generated
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column firm_offer_movements.ex_change
     *
     * @return the value of firm_offer_movements.ex_change
     * @mbg.generated
     */
    public String getExChange() {
        return exChange;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column firm_offer_movements.ex_change
     *
     * @param exChange the value for firm_offer_movements.ex_change
     * @mbg.generated
     */
    public void setExChange(String exChange) {
        this.exChange = exChange == null ? null : exChange.trim();
    }
}