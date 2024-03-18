package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.Button;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ButtonRepository extends JpaRepository<Button, Integer> {
    public List<Button> findAllByBelongsToMenu(String belongsToMenu);

    List<Button> findAllByBelongsToMenuStartingWith(String startingWith);

    Button findButtonByCallbackData(String callbackData);
    void deleteAllByBelongsToMenu(String belongsToMenu);

    void deleteAllByName(String name);
}
