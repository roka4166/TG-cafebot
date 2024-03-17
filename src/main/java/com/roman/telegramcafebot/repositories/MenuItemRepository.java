package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, Integer> {
    List<MenuItem> findAllByBelongsToMenuAndIsStoppedFalse(String belongsToMenu);

    MenuItem findMenuItemByName(String name);

    MenuItem findMenuItemByBelongsToMenu(String belongsToMenu);

    MenuItem findTopByBelongsToMenuAndChatIdOrderByIdDesc(String belongsToMenu, Long chatId);

    MenuItem findTopByChatIdOrderByIdDesc(Long chatId);

    List<MenuItem> findAllByIsStoppedTrue();
}
