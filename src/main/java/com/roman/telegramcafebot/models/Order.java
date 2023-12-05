package com.roman.telegramcafebot.models;

import com.roman.telegramcafebot.models.Button;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "order")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "chatId")
    private Integer chatId;
    @Column(name = "totalPrice")
    private Integer totalPrice;

    @Column(name = "items")
    private String items;

    public Order() {
    }

    public Order(Integer id, Integer chatId, Integer totalPrice, String items) {
        this.id = id;
        this.chatId = chatId;
        this.totalPrice = totalPrice;
        this.items = items;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getChatId() {
        return chatId;
    }

    public void setChatId(Integer chatId) {
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
}
