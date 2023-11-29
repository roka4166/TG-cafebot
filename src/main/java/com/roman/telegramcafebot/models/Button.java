package com.roman.telegramcafebot.models;

import jakarta.persistence.*;

public class Button {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    @Column(name = "name")
    private String name;
    @Column(name = "belongsToMenu")
    private String belongsToMenu;

    @ManyToOne
    @JoinColumn(name = "menu", referencedColumnName = "id")
    private Menu menu;

    public Button(int id, String name, String belongsToMenu) {
        this.id = id;
        this.name = name;
        this.belongsToMenu = belongsToMenu;
    }

    public Button() {
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
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
