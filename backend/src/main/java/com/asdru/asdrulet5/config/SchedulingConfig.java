package com.asdru.asdrulet5.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Backs delayed, off-request-thread work — e.g. PartyService holding a
 * won fight on-screen for a few seconds before returning the party to the
 * dungeon. A single thread is plenty: these are occasional, short-lived
 * delayed callbacks, not a real task queue.
 */
@Configuration
public class SchedulingConfig {

    @Bean
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
