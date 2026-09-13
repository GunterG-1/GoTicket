package com.GoTicket.Orden.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.GoTicket.Orden.Model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findTop50ByPublishedFalseOrderByCreatedAtAsc();
}