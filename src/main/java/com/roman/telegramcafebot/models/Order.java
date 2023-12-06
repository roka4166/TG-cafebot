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
    private Integer chatId;
    @Column(name = "total_price")
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

    @Override
    public String toString() {
        return "Order{" +
                "totalPrice=" + totalPrice +
                ", items='" + items + '\'' +
                '}';
    }
}
