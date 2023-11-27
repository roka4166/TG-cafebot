package com.roman.telegramcafebot.repositories;

import com.roman.telegramcafebot.utils.AdminKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminKeyRepository extends JpaRepository<AdminKey, Integer> {

}
