package com.roman.telegramcafebot.models;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_password")
public class AdminPassword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "password")
    private String key;

    public AdminPassword(Integer id, String key, Boolean isActive) {
        this.id = id;
        this.key = key;

    }

    public AdminPassword() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
