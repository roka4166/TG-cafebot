package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.Button;
import com.roman.telegramcafebot.models.Coworker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoworkerRepository extends JpaRepository<Coworker, Integer> {
   Coworker findCoworkerByChatId(Long chatId);

   Coworker findCoworkerByIsActive(Boolean active);
}
