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
    private String itemsId;

    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    @Column(name = "price")
    private Integer price;
    @Column(name = "items_name")
    private String itemsName;
    @Column(name = "belongs_to_menu")
    private String belongsToMenu;



    public Cart() {
    }

    public Cart(int id, Long chatId, String itemsId, LocalDateTime expirationDate,
                Integer price, String itemsName, String belongsToMenu) {
        this.id = id;
        this.chatId = chatId;
        this.itemsId = itemsId;
        this.expirationDate = expirationDate;
        this.price = price;
        this.itemsName = itemsName;
        this.belongsToMenu = belongsToMenu;
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

    public String getItemsId() {
        return itemsId;
    }

    public void setItemsId(String itemsId) {
        this.itemsId = itemsId;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getItemsName() {
        return itemsName;
    }

    public void setItemsName(String itemsName) {
        this.itemsName = itemsName;
    }

    public String getBelongsToMenu() {
        return belongsToMenu;
    }

    public void setBelongsToMenu(String belongsToMenu) {
        this.belongsToMenu = belongsToMenu;
    }
}
