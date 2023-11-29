package com.roman.telegramcafebot.models;

import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.List;

@Table(name = "menu")
public class Menu {
    private int id;

    private String typeOfMenu;
    @OneToMany(mappedBy = "loaner")
    private List<Button> buttons;

    public Menu(int id, String typeOfMenu) {
        this.id = id;
        this.typeOfMenu = typeOfMenu;
    }

    public Menu() {
    }

    public List<Button> getButtons() {
        return buttons;
    }

    public void setButtons(List<Button> buttons) {
        this.buttons = buttons;
    }

    public String getTypeOfMenu() {
        return typeOfMenu;
    }

    public void setTypeOfMenu(String typeOfMenu) {
        this.typeOfMenu = typeOfMenu;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
