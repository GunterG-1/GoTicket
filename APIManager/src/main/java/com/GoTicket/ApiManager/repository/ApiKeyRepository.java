package com.GoTicket.ApiManager.repository;

import com.GoTicket.ApiManager.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyString(String keyString);
    boolean existsByKeyString(String keyString);
}
