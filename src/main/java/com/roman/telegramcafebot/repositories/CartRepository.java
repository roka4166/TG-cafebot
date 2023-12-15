package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    List<Cart> findAllByChatId(Long chatId);

    void deleteAllByChatId(Long chatId);

    void deleteByItemsId(Integer itemsIs);
}
