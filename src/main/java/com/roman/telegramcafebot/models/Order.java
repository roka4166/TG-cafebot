package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
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

    public Order() {
    }

    public Order(Integer id, Long chatId, Integer totalPrice, String items, String time) {
        this.id = id;
        this.chatId = chatId;
        this.totalPrice = totalPrice;
        this.items = items;
        this.time = time;
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

    @Override
    public String toString() {
        return String.format("Заказ номер #%d на сумму %d руб. Содержит: %s. Время: %s", id, totalPrice, items, getTime());
    }
}
