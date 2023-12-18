package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;

@Component
@Entity
@Table(name = "reservation")
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "chat_id")
    private Long chatId;
    @Column(name = "table_number")
    private Integer table;
    @Column(name = "name")
    private String name;
    @Column(name = "time")
    private String time;
    @Column(name = "confirmed")
    private Boolean confirmed;
    @Column(name = "comment")
    private String coworkerComment;

    public Reservation(Integer id, Long customerChatId, Integer table, String name, String time, Boolean reservationConfirmed, String coworkerComment) {
        this.id = id;
        this.chatId = customerChatId;
        this.table = table;
        this.name = name;
        this.time = time;
        this.confirmed = reservationConfirmed;
        this.coworkerComment = coworkerComment;
    }

    public Reservation() {
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

    public void setChatId(Long customerChatId) {
        this.chatId = customerChatId;
    }

    public Integer getTable() {
        return table;
    }

    public void setTable(Integer table) {
        this.table = table;
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

    public Boolean getReservationConfirmed() {
        return confirmed;
    }

    public void setReservationConfirmed(Boolean reservationConfirmed) {
        this.confirmed = reservationConfirmed;
    }

    public String getCoworkerComment() {
        return coworkerComment;
    }

    public void setCoworkerComment(String coworkerComment) {
        this.coworkerComment = coworkerComment;
    }

    @Override
    public String toString() {
        return "Бронь стола " + table + " на имя " + name + ". Время " + time;
    }
}
