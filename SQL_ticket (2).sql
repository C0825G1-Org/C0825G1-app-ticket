-- 2. SQL tạo Database
Drop database if exists event_booking_db;
CREATE DATABASE if not exists event_booking_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE event_booking_db;

-- 3. Người dùng & phân quyền
-- 3.1 Roles
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL -- ADMIN, USER
);

-- 3.2 Users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number varchar(225),
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3.3 User – Role (Many-to-Many)
CREATE TABLE user_roles (
    user_id BIGINT,
    role_id BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 4. Loại sự kiện
CREATE TABLE event_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT
);

-- 5. Sự kiện
CREATE TABLE events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category_id BIGINT,
    created_by BIGINT,
    organizer_id BIGINT,
    status ENUM('PENDING','APPROVED','REJECTED', 'CANCELLED', 'HAPPENING', 'FINISHED', 'DELETED') DEFAULT 'PENDING',
    view_count BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES event_categories(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (organizer_id) REFERENCES users(id)
);

-- 6. Thời gian diễn ra sự kiện
CREATE TABLE event_times (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT,
    start_time DATETIME,
    end_time DATETIME,
    FOREIGN KEY (event_id) REFERENCES events(id)
);
-- 13.1 Bảng Tỉnh / Thành phố
CREATE TABLE provinces (
    code INT PRIMARY KEY, -- Mã chuẩn (VD: 1 cho Hà Nội)
    name VARCHAR(100) NOT NULL    -- Tên Tỉnh (VD: Thành phố Hà Nội)
);
-- 13.2 Bảng Phường / Xã (Bỏ qua Quận/Huyện, Nối trực tiếp Tỉnh)
CREATE TABLE wards (
    code INT PRIMARY KEY, -- Mã chuẩn của xã/phường
    name VARCHAR(100) NOT NULL,   -- Tên xã/phường
    province_code INT,    -- Nối trực tiếp lên Tỉnh
    FOREIGN KEY (province_code) REFERENCES provinces(code)
);
-- 13.3 Bảng Địa điểm cụ thể (Nơi tổ chức)
CREATE TABLE locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ward_code INT NOT NULL,         -- Nối tới Phường/Xã
    address_detail VARCHAR(255) NOT NULL,   -- Số nhà, tên đường
    map_link TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ward_code) REFERENCES wards(code)
);
CREATE TABLE event_occurrences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    location_id BIGINT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    FOREIGN KEY (location_id) REFERENCES locations(id),
    CONSTRAINT unique_event_slot UNIQUE (location_id, start_time)
);
-- 7. Loại vé
CREATE TABLE ticket_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_occurrence_id BIGINT,
    name VARCHAR(100),
    price DECIMAL(10,2),
    quantity INT,
    FOREIGN KEY (event_occurrence_id) REFERENCES event_occurrences(id) ON DELETE CASCADE
);

-- 8. Đặt vé
-- 8.1 Booking
CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    booking_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status ENUM('SUCCESS','CANCELLED','PENDING'),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 8.2 Chi tiết booking
CREATE TABLE booking_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT,
    ticket_type_id BIGINT,
    quantity INT,
    FOREIGN KEY (booking_id) REFERENCES bookings(id),
    FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id)
);

-- 9. Vé & QR Code
-- 9.1 Vé
CREATE TABLE tickets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_detail_id BIGINT,
    ticket_code VARCHAR(255) UNIQUE,
    `used` BIT(1) DEFAULT b'0',
  `check_in_time` DATETIME DEFAULT NULL,
  CONSTRAINT `fk_tickets_booking_details` FOREIGN KEY (`booking_detail_id`) REFERENCES `booking_details` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 9.2 QR Code
CREATE TABLE qr_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT,
    qr_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

-- 10. Check in QR
CREATE TABLE check_ins (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT,
    check_in_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_id) REFERENCES tickets(id)
);

-- 11. Duyệt yêu cầu tạo sự kiện (Admin)
CREATE TABLE event_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT,
    admin_id BIGINT,
    decision ENUM('APPROVED','REJECTED'),
    note TEXT,
    decision_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id),
    FOREIGN KEY (admin_id) REFERENCES users(id)
);

-- 11.2 Lịch sử hủy/từ chối sự kiện
CREATE TABLE event_cancellation_histories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 12. Hình ảnh và Video quảng cáo cho sự kiện
CREATE TABLE event_media (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    media_url VARCHAR(500) NOT NULL,
    media_type ENUM('IMAGE', 'VIDEO') NOT NULL,
    -- Thêm cột mục đích sử dụng
    media_purpose ENUM('BANNER', 'LOGO', 'TICKET_MAP', 'GALLERY') NOT NULL, 
    is_thumbnail BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE
);

-- ============================================================================
-- 13. ĐỊA GIỚI HÀNH CHÍNH (Dùng cho Select Option)
-- ============================================================================

