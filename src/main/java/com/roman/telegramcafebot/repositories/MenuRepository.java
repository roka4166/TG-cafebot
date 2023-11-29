package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.models.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuRepository extends JpaRepository<MenuItem, Integer>{

}
