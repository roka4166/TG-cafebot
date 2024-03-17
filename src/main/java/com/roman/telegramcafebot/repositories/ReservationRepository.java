package com.roman.telegramcafebot.repositories;
import com.roman.telegramcafebot.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    Reservation findTopByChatIdOrderByIdDesc(Long chatId);
    List<Reservation> findAllByChatIdAndExpirationDateAfterAndConfirmedByCoworkerTrueOrderById(Long chatId, LocalDateTime timeNow);

    Reservation findReservationByCoworkerCommentContaining(String comment);

    Reservation findTopByCoworkerCommentContainingOrderByIdDesc (String comment);
    Reservation findReservationByCoworkerCommentStartingWith(String comment);
}
