package com.roman.telegramcafebot.utils;

import com.roman.telegramcafebot.models.Button;

import java.util.ArrayList;
import java.util.List;

public class Order {
    private int total = 0;

    private List<String> addedItems = new ArrayList<>();


    public void setAddedItems(List<String> addedItems) {
        this.addedItems = addedItems;
    }

    public int getTotal() {
        return total;
    }

    public List<String> getAddedItems() {
        return addedItems;
    }

    public void addToTotal(int num){
        total += num;
    }

    @Override
    public String toString() {
        return "Order{" +
                "total=" + total +
                ", addedItems=" + addedItems +
                '}';
    }
}
