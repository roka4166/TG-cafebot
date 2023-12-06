package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "button")
public class Button {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "belongs_to_menu")
    private String belongsToMenu;
    @Column(name = "callbackdata")
    private String callbackData;

    public Button(int id, String name, String belongsToMenu, String callbackData) {
        this.id = id;
        this.name = name;
        this.belongsToMenu = belongsToMenu;
        this.callbackData = callbackData;
    }

    public Button() {
    }

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBelongsToMenu() {
        return belongsToMenu;
    }

    public void setBelongsToMenu(String belongsToMenu) {
        this.belongsToMenu = belongsToMenu;
    }
}
