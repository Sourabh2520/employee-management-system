-- MySQL setup (optional: the app also creates the database automatically)
-- Run in MySQL Workbench or: mysql -u root -p < db/setup.sql
CREATE DATABASE IF NOT EXISTS employee_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE employee_db;

-- Tables are created automatically by Hibernate (spring.jpa.hibernate.ddl-auto=update).
-- Equivalent structure, for reference / manual setup:
CREATE TABLE IF NOT EXISTS employees (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  employee_id     VARCHAR(20) UNIQUE,
  full_name       VARCHAR(100) NOT NULL,
  email           VARCHAR(150) NOT NULL UNIQUE,
  phone_number    VARCHAR(20)  NOT NULL,
  department      VARCHAR(80)  NOT NULL,
  designation     VARCHAR(80)  NOT NULL,
  date_of_joining DATE         NOT NULL,
  salary          DECIMAL(12,2) NOT NULL,
  status          VARCHAR(10)  NOT NULL
);
