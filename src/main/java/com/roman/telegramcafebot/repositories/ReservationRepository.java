package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {
    Reservation findReservationByChatId(Long chatId);

    Reservation findReservationByCoworkerComment(String startingWith);

    void removeAllByChatId(Long chatId);
}
