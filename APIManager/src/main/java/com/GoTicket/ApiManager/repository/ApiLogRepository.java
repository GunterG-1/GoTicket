package com.GoTicket.ApiManager.repository;

import com.GoTicket.ApiManager.model.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {
    List<ApiLog> findTop50ByOrderByIdDesc();
}
