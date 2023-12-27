package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;

@Component
public class Reservation {
    private Long chatId;
    private String amountOfPeople;
    private String name;
    private String time;
    private String coworkerComment;

    public Reservation() {
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long customerChatId) {
        this.chatId = customerChatId;
    }

    public String getAmountOfPeople() {
        return amountOfPeople;
    }

    public void setAmountOfPeople(String amountOfPeople) {
        this.amountOfPeople = amountOfPeople;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getCoworkerComment() {
        return coworkerComment;
    }

    public void setCoworkerComment(String coworkerComment) {
        this.coworkerComment = coworkerComment;
    }

    @Override
    public String toString() {
        return "Бронь стола. Количество человек" + amountOfPeople + " на имя " + name + ". Время " + time;
    }
}
