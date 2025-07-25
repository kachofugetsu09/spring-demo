-- 创建buildings表
CREATE TABLE IF NOT EXISTS buildings (
    id VARCHAR(32) PRIMARY KEY COMMENT '建筑ID',
    name VARCHAR(100) NOT NULL COMMENT '建筑名称',
    type VARCHAR(50) NOT NULL COMMENT '建筑类型',
    x DOUBLE NOT NULL COMMENT 'X坐标',
    y DOUBLE NOT NULL COMMENT 'Y坐标',
    z DOUBLE NOT NULL COMMENT 'Z坐标',
    roll DOUBLE NOT NULL DEFAULT 0 COMMENT '翻滚角',
    pitch DOUBLE NOT NULL DEFAULT 0 COMMENT '俯仰角',
    yaw DOUBLE NOT NULL DEFAULT 0 COMMENT '偏航角',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='建筑信息表';

-- 插入测试数据
INSERT INTO buildings (id, name, type, x, y, z, roll, pitch, yaw) VALUES 
('1', '主楼', 'office', 100.0, 200.0, 0.0, 0.0, 0.0, 0.0),
('2', '实验楼', 'laboratory', 150.0, 250.0, 0.0, 0.0, 0.0, 90.0),
('3', '图书馆', 'library', 200.0, 300.0, 0.0, 0.0, 0.0, 180.0)
ON DUPLICATE KEY UPDATE 
name = VALUES(name),
type = VALUES(type),
x = VALUES(x),
y = VALUES(y),
z = VALUES(z),
roll = VALUES(roll),
pitch = VALUES(pitch),
yaw = VALUES(yaw);
