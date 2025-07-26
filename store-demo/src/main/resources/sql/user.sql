CREATE TABLE IF NOT EXISTS `users` (

                                       `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',

                                       `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',

    `password` VARCHAR(255) NOT NULL COMMENT '密码',

    `phone` VARCHAR(20) COMMENT '手机号码',

    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`)

    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';


CREATE TABLE IF NOT EXISTS `roles` (
                                       `role_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色ID',
                                       `role_name` VARCHAR(50) NOT NULL UNIQUE COMMENT '角色名称',
    PRIMARY KEY (`role_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';


CREATE TABLE IF NOT EXISTS `user_roles` (
                                            `user_id` BIGINT NOT NULL COMMENT '用户ID',
                                            `role_id` BIGINT NOT NULL COMMENT '角色ID',
                                            PRIMARY KEY (`user_id`, `role_id`), -- 联合主键
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (`role_id`) REFERENCES `roles`(`role_id`) ON DELETE CASCADE ON UPDATE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';