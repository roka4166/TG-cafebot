package com.roman.telegramcafebot.repositories;
import com.roman.telegramcafebot.models.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Order findOrderByChatId(Long chatId);

    @Query("SELECT o FROM Order o WHERE o.chatId = :chatId ORDER BY o.id DESC")
    Order findLastOrderByCustomerId(Long chatId);

    Order findTopByChatIdOrderByIdDesc(long chatId);

}
