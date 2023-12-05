package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.Coworker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Coworker, Integer> {

}
