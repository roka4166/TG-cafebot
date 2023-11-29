package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    MenuItem findByName(String itemName);

    List<MenuItem> findAllByType(String type);
}
