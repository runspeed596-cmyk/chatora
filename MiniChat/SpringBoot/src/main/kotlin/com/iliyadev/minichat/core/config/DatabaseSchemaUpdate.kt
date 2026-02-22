package com.iliyadev.minichat.core.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseSchemaUpdate(private val jdbcTemplate: JdbcTemplate) {
    private val logger = LoggerFactory.getLogger(DatabaseSchemaUpdate::class.java)

    @PostConstruct
    fun updateSchema() {
        try {
            logger.info("Checking for missing columns in 'users' table...")
            
            // Check for is_premium
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN is_premium BOOLEAN DEFAULT FALSE")
                logger.info("Added column 'is_premium' to 'users' table.")
            } catch (e: Exception) {
                if (e.message?.contains("already exists", ignoreCase = true) == true) {
                    logger.info("Column 'is_premium' already exists.")
                } else {
                    logger.warn("Failed to add 'is_premium': ${e.message}")
                }
            }

            // Check for premium_until
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN premium_until TIMESTAMP WITH TIME ZONE NULL")
                logger.info("Added column 'premium_until' to 'users' table.")
            } catch (e: Exception) {
                if (e.message?.contains("already exists", ignoreCase = true) == true) {
                    logger.info("Column 'premium_until' already exists.")
                } else {
                    logger.warn("Failed to add 'premium_until'. It might already exist or be managed by Hibernate: ${e.message}")
                }
            }

            logger.info("Database schema update check completed.")
        } catch (e: Exception) {
            logger.error("Error during schema update: ${e.message}")
        }
    }
}
