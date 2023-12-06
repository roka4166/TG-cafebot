package com.roman.telegramcafebot.models;

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
    @Column(name = "isActive")
    private Boolean IsActive;

    public AdminKey(Integer id, String key, Boolean isActive) {
        this.id = id;
        this.key = key;
        IsActive = isActive;
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

    public Boolean getActive() {
        return IsActive;
    }

    public void setActive(Boolean active) {
        IsActive = active;
    }
}
