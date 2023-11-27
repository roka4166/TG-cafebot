package com.roman.telegramcafebot.utils;

import jakarta.persistence.*;

@Entity
@Table(name = "admin_key")
public class AdminKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;
    @Column(name = "key")
    private String key;

    public AdminKey(Integer id, String key) {
        this.id = id;
        this.key = key;
    }

    public AdminKey() {
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