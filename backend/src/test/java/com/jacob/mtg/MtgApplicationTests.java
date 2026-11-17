package com.jacob.mtg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the full Spring context. Needs the Postgres from docker-compose running.
 * Later: replace with Testcontainers so tests start their own throwaway Postgres.
 */
@SpringBootTest
class MtgApplicationTests {

    @Test
    void contextLoads() {
    }
}
