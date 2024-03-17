package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Entity
@Component
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "amount_of_people")
    private String amountOfPeople;
    @Column(name = "name")
    private String name;
    @Column(name = "time")
    private String time;
    @Column(name = "coworker_comment")
    private String coworkerComment;
    @Column(name = "confirmed_by_coworker")
    private Boolean confirmedByCoworker;
    @Column(name = "sent_to_coworker")
    private Boolean sentToCoworker;
    @Column(name = "expiration_date")
    private LocalDateTime expirationDate;

    public Reservation() {
    }

    public Reservation(Integer id, Long chatId, String amountOfPeople,
                       String name, String time, String coworkerComment,
                       Boolean confirmedByCoworker, Boolean sentToCoworker,
                       LocalDateTime expirationDate) {
        this.id = id;
        this.chatId = chatId;
        this.amountOfPeople = amountOfPeople;
        this.name = name;
        this.time = time;
        this.coworkerComment = coworkerComment;
        this.confirmedByCoworker = confirmedByCoworker;
        this.sentToCoworker = sentToCoworker;
        this.expirationDate = expirationDate;
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

    public Boolean getConfirmedByCoworker() {
        return confirmedByCoworker;
    }

    public void setConfirmedByCoworker(Boolean confirmedByCoworker) {
        this.confirmedByCoworker = confirmedByCoworker;
    }

    public Boolean getSentToCoworker() {
        return sentToCoworker;
    }

    public void setSentToCoworker(Boolean sentToCoworker) {
        this.sentToCoworker = sentToCoworker;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public String toString() {
        return "Бронь стола. Количество человек " + amountOfPeople + " на имя " + name + ". Время " + time;
    }
}
