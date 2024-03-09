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
    private String password;

    public AdminPassword(Integer id, String password, Boolean isActive) {
        this.id = id;
        this.password = password;

    }

    public AdminPassword() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String key) {
        this.password = key;
    }
}
