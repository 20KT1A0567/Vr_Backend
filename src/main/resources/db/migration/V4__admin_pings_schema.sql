CREATE TABLE admin_pings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_email VARCHAR(100) NOT NULL,
    sender_name VARCHAR(100),
    sender_role VARCHAR(50),
    message TEXT NOT NULL,
    ping_timestamp DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
