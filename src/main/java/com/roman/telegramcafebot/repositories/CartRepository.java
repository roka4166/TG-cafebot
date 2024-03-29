package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {

    List<Cart> findAllByChatId(Long chatId);

    List<Cart> findByChatIdAndAndItemsId(Long chatId, String itemsId);
    Cart findTopByChatIdAndItemsIdOrderByIdDesc(Long chatId, String itemsId);

    void deleteAllByChatId(Long chatId);

    void deleteByChatIdAndItemsId(Long chatId, String itemsId);
    List<Cart> findAllByChatIdOrderByIdDesc(Long chatId);


    void deleteByItemsId(String itemsIs);
}
