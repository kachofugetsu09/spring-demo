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