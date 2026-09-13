package com.GoTicket.ApiManager.repository;

import com.GoTicket.ApiManager.model.ApiRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApiRouteRepository extends JpaRepository<ApiRoute, Long> {
    Optional<ApiRoute> findByPathPattern(String pathPattern);
    boolean existsByPathPattern(String pathPattern);
}
