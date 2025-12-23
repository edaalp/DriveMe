package com.driveme.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing for created/updated timestamps.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
