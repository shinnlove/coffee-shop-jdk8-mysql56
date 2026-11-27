-- 使用本机 MySQL 管理员执行；适用于 MySQL 5.6.30 / 5.7。
-- 只创建专用数据库和账号，不创建业务表，不删除已有库或修改已有账号密码。
-- 下方密码仅用于本地演示。请在同一连接中顺序执行整个文件。
CREATE DATABASE IF NOT EXISTS coffee_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE DATABASE IF NOT EXISTS coffee_shop_test CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- MySQL 5.6 不支持 CREATE USER IF NOT EXISTS。
-- 先检查账号是否存在，再执行 CREATE USER 或空操作 DO 0，避免重复建账号报错。

-- coffee_app@localhost：只授予 coffee_shop 数据库权限。
SET @coffee_create_account_sql = IF(
    EXISTS(SELECT 1 FROM mysql.user WHERE User = 'coffee_app' AND Host = 'localhost'),
    'DO 0',
    'CREATE USER ''coffee_app''@''localhost'' IDENTIFIED BY ''coffee-local-only'''
);
PREPARE coffee_create_account FROM @coffee_create_account_sql;
EXECUTE coffee_create_account;
DEALLOCATE PREPARE coffee_create_account;
GRANT ALL PRIVILEGES ON `coffee\_shop`.* TO 'coffee_app'@'localhost';

-- coffee_app@127.0.0.1：只授予 coffee_shop 数据库权限。
SET @coffee_create_account_sql = IF(
    EXISTS(SELECT 1 FROM mysql.user WHERE User = 'coffee_app' AND Host = '127.0.0.1'),
    'DO 0',
    'CREATE USER ''coffee_app''@''127.0.0.1'' IDENTIFIED BY ''coffee-local-only'''
);
PREPARE coffee_create_account FROM @coffee_create_account_sql;
EXECUTE coffee_create_account;
DEALLOCATE PREPARE coffee_create_account;
GRANT ALL PRIVILEGES ON `coffee\_shop`.* TO 'coffee_app'@'127.0.0.1';

-- coffee_test@localhost：只授予 coffee_shop_test 数据库权限。
SET @coffee_create_account_sql = IF(
    EXISTS(SELECT 1 FROM mysql.user WHERE User = 'coffee_test' AND Host = 'localhost'),
    'DO 0',
    'CREATE USER ''coffee_test''@''localhost'' IDENTIFIED BY ''coffee-test-only'''
);
PREPARE coffee_create_account FROM @coffee_create_account_sql;
EXECUTE coffee_create_account;
DEALLOCATE PREPARE coffee_create_account;
GRANT ALL PRIVILEGES ON `coffee\_shop\_test`.* TO 'coffee_test'@'localhost';

-- coffee_test@127.0.0.1：只授予 coffee_shop_test 数据库权限。
SET @coffee_create_account_sql = IF(
    EXISTS(SELECT 1 FROM mysql.user WHERE User = 'coffee_test' AND Host = '127.0.0.1'),
    'DO 0',
    'CREATE USER ''coffee_test''@''127.0.0.1'' IDENTIFIED BY ''coffee-test-only'''
);
PREPARE coffee_create_account FROM @coffee_create_account_sql;
EXECUTE coffee_create_account;
DEALLOCATE PREPARE coffee_create_account;
GRANT ALL PRIVILEGES ON `coffee\_shop\_test`.* TO 'coffee_test'@'127.0.0.1';

-- 账号已存在时，仍使用它的原密码，启动前请用 DB_PASSWORD / TEST_DB_PASSWORD 配置。
-- 启动应用后，Liquibase 自动执行唯一的 V1__schema.sql，创建表并插入参考数据。
