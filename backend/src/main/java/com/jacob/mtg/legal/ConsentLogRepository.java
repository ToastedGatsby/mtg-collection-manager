package com.jacob.mtg.legal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentLogRepository extends JpaRepository<ConsentLog, Long> {

    List<ConsentLog> findByUserId(Long userId);
}
