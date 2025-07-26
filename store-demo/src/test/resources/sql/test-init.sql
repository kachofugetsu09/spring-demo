-- 创建测试数据库
CREATE DATABASE IF NOT EXISTS store_demo_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE store_demo_test;

-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    phone VARCHAR(20) COMMENT '手机号',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 创建角色表
CREATE TABLE IF NOT EXISTS roles (
    role_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    PRIMARY KEY (role_id),
    UNIQUE KEY uk_role_name (role_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 创建用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 插入测试角色
INSERT INTO roles (role_name) VALUES 
    ('ADMIN'),
    ('USER'),
    ('MANAGER')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

-- 插入测试用户 (密码为 123456 的BCrypt加密结果)
INSERT INTO users (username, password, phone) VALUES 
    ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIFi', '13800138000'),
    ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIFi', '13800138001'),
    ('manager', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIFi', '13800138002')
ON DUPLICATE KEY UPDATE 
    password = VALUES(password),
    phone = VALUES(phone),
    update_time = CURRENT_TIMESTAMP;

-- 分配用户角色
INSERT INTO user_roles (user_id, role_id) VALUES 
    (1, 1), -- admin用户分配ADMIN角色
    (1, 3), -- admin用户分配MANAGER角色
    (2, 2), -- user用户分配USER角色
    (3, 3)  -- manager用户分配MANAGER角色
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);