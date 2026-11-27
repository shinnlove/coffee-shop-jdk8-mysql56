-- 单一 V1 初始化基线：10 张业务表 + PRD 参考数据。目标环境 MySQL 5.6.30 / InnoDB。
-- 所有字段和索引直接使用最终定义，逻辑关联不建立物理外键。
-- 由应用启动时的 Liquibase 在空库执行一次；不要再手工导入本文件。

-- 01. 店铺配置表
-- 当前单店，初始化 id=1；没有额外业务编码需求，不增加 shop_code。
CREATE TABLE `shop` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `currency` CHAR(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '店铺币种，如USD',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '店铺配置表';

-- 02. 商品分类表
-- 供其他表关联的是 id；category_code 仅用于原接口字符串标识的兼容映射。
-- uk_category_code 保证对外标识查找唯一，不用于表间关联。
CREATE TABLE `category` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `category_code` VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '原接口分类标识，如beverages；仅用于对外兼容',
    `name` VARCHAR(120) NOT NULL COMMENT '分类名称',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_category_code` (`category_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '商品分类表';

-- 03. 菜单商品表
-- 逻辑关联：category_id -> category.id。
-- id 是商品内部标识；item_code 保留原对外商品标识，仅存于本表。
-- idx_category_id 支持按分类主键查询商品；uk_item_code 支持外部请求标识到内部 id 的查找。
CREATE TABLE `menu_item` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `item_code` VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '原接口商品标识，如cappuccino；仅用于对外兼容',
    `category_id` BIGINT UNSIGNED NOT NULL COMMENT '分类主键，逻辑关联category.id',
    `name` VARCHAR(120) NOT NULL COMMENT '商品名称',
    `price_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '商品单价，最小货币单位；USD时为美分',
    `active` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '上架状态：0-下架，1-上架',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_item_code` (`item_code`),
    INDEX `idx_category_id` (`category_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '菜单商品表';

-- 04. 商品库存表
-- 逻辑关联：item_id -> menu_item.id。
-- id 是库存记录主键；item_id 直接使用已有商品主键，不生成另一个商品编号。
-- 当前单库存池，uk_item_id 保证一个商品一条库存记录；仍按 item_id 的唯一索引锁库存。
-- 创建订单不预占库存，支付成功时才扣减。
CREATE TABLE `item_inventory` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `item_id` BIGINT UNSIGNED NOT NULL COMMENT '商品主键，逻辑关联menu_item.id',
    `stock_quantity` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前可售库存，不含预占逻辑',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_item_id` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '商品库存表';

-- 05. 商品组合优惠表
-- 逻辑关联：first_item_id、second_item_id -> menu_item.id。
-- offer_code 只用于原优惠接口标识兼容；商品关系全部改为整型主键关联。
-- 启用规则中两个商品不同、优惠不能重叠使用同一商品等校验仍由应用执行。
CREATE TABLE `combo_offer` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `offer_code` VARCHAR(80) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '原接口优惠标识，如coffee-pastry；仅用于对外兼容',
    `name` VARCHAR(120) NOT NULL COMMENT '组合优惠名称',
    `first_item_id` BIGINT UNSIGNED NOT NULL COMMENT '第一商品主键，逻辑关联menu_item.id',
    `second_item_id` BIGINT UNSIGNED NOT NULL COMMENT '第二商品主键，逻辑关联menu_item.id',
    `bundle_price_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '一套组合的成交价格，最小货币单位',
    `active` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '启用状态：0-停用，1-启用',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_offer_code` (`offer_code`),
    INDEX `idx_first_item_id` (`first_item_id`),
    INDEX `idx_second_item_id` (`second_item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '商品组合优惠表';

-- 06. 顾客订单主表
-- id 是自增订单主键，原项目中的 sequence_no；order_uuid 保存原对外 UUID。
-- 子表 order_id 统一指向这里的 id，不指向 order_uuid。
-- 展示订单号仍为 C- 加 id；没有另增订单编号生成机制。
-- idx_status_paid_at_id 可服务于状态筛选及 paid_at、id 数值顺序。
-- 当前接口按 paidAt、orderNumber 排序；若 SQL 仍以 CONCAT('C-', id) 排序，
-- 不能认为该索引完全消除了排序；本轮不改变原有对外排序语义。
CREATE TABLE `customer_order` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID，原sequence_no，也是可读订单号序号来源',
    `order_uuid` CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '订单对外UUID标识；接口id仍使用此值，内部关联使用整型主键',
    `customer_name` VARCHAR(80) NOT NULL COMMENT '顾客取餐姓名，不是用户账号标识',
    `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单状态：1-待支付PENDING_PAYMENT，2-已支付PAID',
    `currency` CHAR(3) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '下单时的币种快照',
    `subtotal_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '优惠前商品金额合计，最小货币单位',
    `discount_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '优惠总金额，最小货币单位',
    `total_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '应付金额，最小货币单位',
    `paid_at` DATETIME(6) DEFAULT NULL COMMENT '支付成功时间，UTC；待支付时为空',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '订单创建时间，UTC；对外映射createdAt',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_uuid` (`order_uuid`),
    INDEX `idx_status_paid_at_id` (`status`, `paid_at`, `id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1001 DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '顾客订单主表';

-- 07. 订单商品明细表
-- 逻辑关联：order_id -> customer_order.id；item_id -> menu_item.id。
-- 名称、单价、数量和行金额仍是订单快照，不从当前商品价格重新计算。
-- uk_order_item 保证同单同商品唯一，也支持按整型 order_id 查询全部明细。
-- idx_item_id 支持按商品主键反查明细；不重复建立单列 order_id 索引。
-- 对外 itemId 由 item_id 找到商品原接口标识 item_code，输出时按原契约约定排序。
CREATE TABLE `order_line` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单主键，逻辑关联customer_order.id',
    `item_id` BIGINT UNSIGNED NOT NULL COMMENT '商品主键，逻辑关联menu_item.id',
    `name` VARCHAR(120) NOT NULL COMMENT '下单时的商品名称快照',
    `unit_price_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '下单时的商品单价快照，最小货币单位',
    `quantity` INT UNSIGNED NOT NULL COMMENT '购买数量，应用要求大于0并校验上限',
    `line_total_minor` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '优惠前行金额，单价乘数量',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_item` (`order_id`, `item_id`),
    INDEX `idx_item_id` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '订单商品明细表';

-- 08. 订单支付小票表
-- 逻辑关联：order_id -> customer_order.id。
-- uk_order_id 保证一张订单最多一张小票；payload 内的订单ID仍是对外UUID。
CREATE TABLE `receipt` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单主键，逻辑关联customer_order.id',
    `payload` MEDIUMTEXT NOT NULL COMMENT '支付成功时的小票JSON快照',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_id` (`order_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '订单支付小票表';

-- 09. 订单库存扣减记录表
-- 逻辑关联：order_id -> customer_order.id；item_id -> menu_item.id。
-- 当前单库存池下，相同 item_id 在 item_inventory 中唯一对应一条库存余额记录。
-- uk_order_item 保证同单同商品只写一次扣减记录，插入失败回滚同事务的库存更新。
-- 当前只记录支付扣减；退款回补、盘点等仍是后续探讨议题。
CREATE TABLE `inventory_change_log` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID',
    `order_id` BIGINT UNSIGNED NOT NULL COMMENT '订单主键，逻辑关联customer_order.id',
    `item_id` BIGINT UNSIGNED NOT NULL COMMENT '商品主键，逻辑关联menu_item.id；按同一item_id对应item_inventory记录',
    `quantity` INT UNSIGNED NOT NULL COMMENT '本次库存扣减数量，保存正数',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_order_item` (`order_id`, `item_id`),
    INDEX `idx_item_id` (`item_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '订单库存扣减记录表';

-- 10. 接口请求幂等记录表
-- id 是记录主键，(scope, request_key) 仍是请求幂等身份，二者不能互相替代。
-- scope 是通用操作范围；支付 scope 继续使用对外订单UUID，不当作整型关系字段。
-- 不新增 order_id 关联列，创建订单请求执行前尚没有订单主键。
-- request_hash 不唯一；不同 key 配合相同内容仍允许独立下单。
CREATE TABLE `idempotency_record` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '自增ID，不作为请求幂等身份',
    `scope` VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '幂等操作范围，如orders:create或orders:{UUID}:pay',
    `request_key` VARBINARY(128) NOT NULL COMMENT '客户端幂等Key，按原始ASCII字节保存',
    `request_hash` CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化请求SHA-256摘要，十六进制字符串',
    `http_status` INT DEFAULT NULL COMMENT '原始HTTP响应状态码；事务内占位阶段为空',
    `response_body` MEDIUMTEXT COMMENT '原始响应JSON；事务内占位阶段为空',
    `create_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间，UTC',
    `update_time` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间，UTC',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_scope_request_key` (`scope`, `request_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = '接口请求幂等记录表';


-- 11. PRD 参考数据：表创建后初始化一次，由同一个 Liquibase changeSet 管理。
-- 保留对外参考编码；表间关系使用父表整型 id。显式列名避免依赖列顺序。
INSERT INTO shop (id, currency) VALUES (1, 'USD');
INSERT INTO category (id, category_code, name) VALUES
 (1, 'beverages', 'Beverages'), (2, 'bakery', 'Bakery');
INSERT INTO menu_item (id, item_code, category_id, name, price_minor, active) VALUES
 (1, 'cappuccino', 1, 'Cappuccino', 450, 1),
 (2, 'croissant', 2, 'Croissant', 350, 1),
 (3, 'cookie', 2, 'Cookie', 250, 1);
INSERT INTO item_inventory (item_id, stock_quantity) VALUES (1, 10), (2, 8), (3, 12);
INSERT INTO combo_offer (offer_code, name, first_item_id, second_item_id, bundle_price_minor, active)
 VALUES ('coffee-pastry', 'Coffee and pastry', 1, 2, 700, 1);
