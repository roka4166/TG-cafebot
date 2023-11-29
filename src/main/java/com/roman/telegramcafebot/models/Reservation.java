package com.roman.telegramcafebot.models;

import org.springframework.stereotype.Component;

@Component
public class Reservation {

    private long customerChatId;

    public long getCustomerChatId() {
        return customerChatId;
    }

    public void setCustomerChatId(long customerChatId) {
        this.customerChatId = customerChatId;
    }

    private int table;

    private String name;

    private String time;

    public int getTable() {
        return table;
    }

    public void setTable(int table) {
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

    @Override
    public String toString() {
        return "Reservation{" +
                "table=" + table +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
