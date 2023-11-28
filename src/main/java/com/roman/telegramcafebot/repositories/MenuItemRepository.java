package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.utils.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    MenuItem findByName(String itemName);
}
