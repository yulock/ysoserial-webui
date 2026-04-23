-- 创建数据库
CREATE DATABASE IF NOT EXISTS payload_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE payload_platform;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码(MD5加密)',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '是否删除(0-未删除, 1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- Payload生成记录表
CREATE TABLE IF NOT EXISTS `payload_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `gadget_chain` varchar(100) NOT NULL COMMENT 'Gadget链名称',
  `command` text NOT NULL COMMENT '执行的命令',
  `payload_type` varchar(20) DEFAULT 'base64' COMMENT 'Payload类型(base64/hex)',
  `payload_data` longblob COMMENT 'Payload数据',
  `result` text COMMENT '测试结果',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` int DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Payload生成记录表';

-- 测试目标表
CREATE TABLE IF NOT EXISTS `test_target` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(100) NOT NULL COMMENT '目标名称',
  `host` varchar(100) NOT NULL COMMENT '目标主机',
  `port` int NOT NULL COMMENT '目标端口',
  `type` varchar(20) DEFAULT 'other' COMMENT '目标类型',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT '0' COMMENT '是否删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试目标表';

-- 插入默认管理员用户(密码: admin123)
INSERT INTO `user` (`username`, `password`, `email`) VALUES 
('admin', '0192023a7bbd73250516f069df18b500', 'admin@example.com');
