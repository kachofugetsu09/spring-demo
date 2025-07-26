-- 创建buildings表 (id 修改为 BIGINT 并设置为 AUTO_INCREMENT)
CREATE TABLE IF NOT EXISTS building (
                                         id BIGINT NOT NULL AUTO_INCREMENT COMMENT '建筑ID', -- 修改点1: 类型改为BIGINT
                                         name VARCHAR(100) NOT NULL COMMENT '建筑名称',
                                         type VARCHAR(50) NOT NULL COMMENT '建筑类型',
                                         x DOUBLE NOT NULL COMMENT 'X坐标',
                                         y DOUBLE NOT NULL COMMENT 'Y坐标',
                                         z DOUBLE NOT NULL COMMENT 'Z坐标',
                                         roll DOUBLE NOT NULL DEFAULT 0 COMMENT '翻滚角',
                                         pitch DOUBLE NOT NULL DEFAULT 0 COMMENT '俯仰角',
                                         yaw DOUBLE NOT NULL DEFAULT 0 COMMENT '偏航角',
                                         created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                         updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                         PRIMARY KEY (id) -- 修改点2: 明确PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建筑信息表';

-- 重置AUTO_INCREMENT值到合理的起始点
ALTER TABLE building AUTO_INCREMENT = 1;

-- 插入测试数据 (不再指定id，让数据库自增)
-- 注意：如果表已存在且有数据，再次运行这些INSERT语句会新增记录，而不是更新。
-- 如果你希望基于'name'等字段进行更新（upsert），则需要在'name'字段上添加UNIQUE约束。
INSERT INTO building (name, type, x, y, z, roll, pitch, yaw) VALUES
                                                                  ('主楼', 'office', 100.0, 200.0, 0.0, 0.0, 0.0, 0.0),
                                                                  ('实验楼', 'laboratory', 150.0, 250.0, 0.0, 0.0, 0.0, 90.0),
                                                                  ('图书馆', 'library', 200.0, 300.0, 0.0, 0.0, 0.0, 180.0)
ON DUPLICATE KEY UPDATE 
    type = VALUES(type),
    x = VALUES(x),
    y = VALUES(y),
    z = VALUES(z),
    roll = VALUES(roll),
    pitch = VALUES(pitch),
    yaw = VALUES(yaw),
    updated_time = CURRENT_TIMESTAMP;
-- 创建用户

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

-- 插入默认角色
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