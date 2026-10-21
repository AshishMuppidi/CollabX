-- Migration: Initial Schema for CollabX
-- Compatible with MySQL 8.0+

CREATE TABLE IF NOT EXISTS rooms (
    room_id VARCHAR(64) NOT NULL,
    code LONGTEXT NULL,
    password_hash VARCHAR(255) NOT NULL,
    creator_username VARCHAR(100) NOT NULL,
    current_language VARCHAR(50) DEFAULT 'javascript',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (room_id),
    INDEX idx_rooms_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id VARCHAR(64) NOT NULL,
    username VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_room FOREIGN KEY (room_id) REFERENCES rooms (room_id) ON DELETE CASCADE,
    INDEX idx_chat_room_sent (room_id, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
