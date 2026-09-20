-- MySQL Table Initialization Schema

-- Drop existing tables if present to ensure matching Foreign Key column types
DROP TABLE IF EXISTS maintenance_logs;
DROP TABLE IF EXISTS sensors;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;

-- 1. Departments Table
CREATE TABLE departments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL UNIQUE,
    location VARCHAR(100),
    budget DECIMAL(12, 2) DEFAULT 0.00
);

-- 2. Users Table (with RBAC roles: ADMIN, ENGINEER, VIEWER)
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    department_id BIGINT,
    CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- 3. Sensors Table
CREATE TABLE sensors (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sensor_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    sensor_type VARCHAR(50) NOT NULL,
    location VARCHAR(100),
    status VARCHAR(20) NOT NULL,
    last_reading DECIMAL(8, 2),
    department_id BIGINT,
    CONSTRAINT fk_sensors_department FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE SET NULL
);

-- 4. Maintenance Logs Table
CREATE TABLE maintenance_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sensor_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    log_date DATETIME NOT NULL,
    description VARCHAR(255) NOT NULL,
    cost DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_logs_sensor FOREIGN KEY (sensor_id) REFERENCES sensors(id) ON DELETE CASCADE,
    CONSTRAINT fk_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
