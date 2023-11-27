package com.roman.telegramcafebot.utils;


import jakarta.persistence.*;

@Entity
@Table(name = "MenuItem")
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "name")
    private String name;

    @Column(name = "price")
    private Integer price;

    @Column(name = "type")
    private String type;

    public MenuItem(Integer id, String name, Integer price, String type) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.type = type;
    }

    public MenuItem() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
