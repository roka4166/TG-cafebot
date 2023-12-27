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
    private Integer id;
    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column(name = "belongs_to_menu")
    private String belongsToMenu;
    @Column(name = "isstopped")
    private Boolean isStopped;

    public void setId(Integer id) {
        this.id = id;
    }

    public Boolean getStopped() {
        return isStopped;
    }

    public void setStopped(Boolean stopped) {
        isStopped = stopped;
    }

    public MenuItem(int id, String name, Integer price, String belongsToMenu, Boolean isStopped) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.belongsToMenu = belongsToMenu;
        this.isStopped = isStopped;
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
