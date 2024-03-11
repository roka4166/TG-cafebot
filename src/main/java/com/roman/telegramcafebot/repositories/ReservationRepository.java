package com.roman.telegramcafebot.repositories;
import com.roman.telegramcafebot.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    Reservation findTopByChatIdOrderByIdDesc(Long chatId);

    Reservation findReservationByCoworkerCommentContaining(String comment);
    Reservation findReservationByCoworkerCommentStartingWith(String comment);
}
