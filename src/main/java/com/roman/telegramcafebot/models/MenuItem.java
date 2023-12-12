package com.roman.telegramcafebot.models;

import jakarta.persistence.*;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "menu_item")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column(name = "belongs_to_menu")
    private String belongsToMenu;

    public MenuItem(int id, String name, Integer price, String belongsToMenu) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.belongsToMenu = belongsToMenu;
    }

    public MenuItem() {
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

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public String getBelongsToMenu() {
        return belongsToMenu;
    }

    public void setBelongsToMenu(String belongsToMenu) {
        this.belongsToMenu = belongsToMenu;
    }
}
