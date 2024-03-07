package com.roman.telegramcafebot.models;

import com.roman.telegramcafebot.models.MenuItem;
import jakarta.persistence.*;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Component
@Table(name = "cart")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "items_id")
    private int itemsId;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;



    public Cart() {
    }

    public Cart(int id, Long chatId, int itemsId, LocalDateTime expirationDate) {
        this.id = id;
        this.chatId = chatId;
        this.itemsId = itemsId;
        this.expirationDate = expirationDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public int getItemsId() {
        return itemsId;
    }

    public void setItemsId(int itemsId) {
        this.itemsId = itemsId;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

}
