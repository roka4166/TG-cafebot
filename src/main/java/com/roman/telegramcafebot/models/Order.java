package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
import org.aspectj.weaver.ast.Or;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "order_info")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "total_price")
    private Integer totalPrice;

    @Column(name = "items")
    private String items;
    @Column(name = "time")
    private String time;
    @Column(name = "sent")
    private Boolean orderSentToCoworker;
    @Column(name = "order_confirmed")
    private Boolean orderConfirmed;
    @Column(name = "takeaway")
    private Boolean takeaway;

    public Order() {
    }

    public Order(Integer id, Long chatId, Integer totalPrice,
                 String items, String time, Boolean orderSentToCoworker,
                 Boolean orderConfirmed, Boolean takeaway) {
        this.id = id;
        this.chatId = chatId;
        this.totalPrice = totalPrice;
        this.items = items;
        this.time = time;
        this.orderSentToCoworker = orderSentToCoworker;
        this.orderConfirmed = orderConfirmed;
        this.takeaway = takeaway;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Integer getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Integer totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public Boolean getOrderSentToCoworker() {
        return orderSentToCoworker;
    }

    public void setOrderSentToCoworker(Boolean orderSentToCoworker) {
        this.orderSentToCoworker = orderSentToCoworker;
    }

    public Boolean getOrderConfirmed() {
        return orderConfirmed;
    }

    public void setOrderConfirmed(Boolean orderConfirmed) {
        this.orderConfirmed = orderConfirmed;
    }

    public Boolean getTakeaway() {
        return takeaway;
    }

    public void setTakeaway(Boolean takeaway) {
        this.takeaway = takeaway;
    }

    @Override
    public String toString() {
        return String.format("Заказ номер #%d на сумму %d руб. Содержит: %s. Время: %s", id, totalPrice, items, getTime());
    }
}
