package com.jacob.mtg.common.web;

import java.sql.Connection;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public smoke-test endpoint used by the frontend home page and by the keep-alive ping in production. */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", databaseStatus(), Instant.now());
    }

    private String databaseStatus() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2) ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    public record HealthResponse(String status, String database, Instant time) {
    }
}
