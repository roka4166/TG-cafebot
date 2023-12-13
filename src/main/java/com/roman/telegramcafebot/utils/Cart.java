package com.roman.telegramcafebot.utils;

import com.roman.telegramcafebot.models.MenuItem;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Component
public class Cart {
    private Long chatId;

    private int totalPrice;

    private List<MenuItem> menuItemList;

    private LocalDateTime timeOfCreation;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public int getTotalPrice() {
        if(totalPrice == 0){
            return 0;
        }
        return totalPrice;
    }

    public void setTotalPrice(int totalPrice) {
        this.totalPrice = totalPrice;
    }

    public List<MenuItem> getMenuItemList() {
        return menuItemList;
    }

    public void setMenuItemList(List<MenuItem> menuItemList) {
        this.menuItemList = menuItemList;
    }

    public LocalDateTime getTimeOfCreation() {
        return timeOfCreation;
    }

    public void setTimeOfCreation(LocalDateTime timeOfCreation) {
        this.timeOfCreation = timeOfCreation;
    }

    public void resetCart(){
        this.chatId = null;
        this.totalPrice = 0;
        this.menuItemList = null;
        this.timeOfCreation = null;
    }

    public Cart() {
        this.menuItemList = new ArrayList<>();
    }

    public boolean checkIfCartIsExpired(){
        return false;
    }
}
