# MyBatis 版本：DAO、DO、XML 与显式事务

当前领域模型和请求/响应类均为独立文件；`ordering.converter.OrderConverter` 为静态转换类，菜单纯映射位于 `catalog.converter.MenuConverter`。持久层使用 MyBatis DAO + DO + XML。

持久层按以下职责划分：Service 处理业务步骤，DAO 声明数据库操作，Mapper XML 保存 SQL 和字段映射，DO 对应表记录。仍使用 JDK 8、Spring Boot 2.7.18、MySQL/InnoDB，六个接口的请求、响应和业务语义保持一致。

## 文件怎么对应

| 层 | 位置 / 示例 | 职责 |
|---|---|---|
| Controller | `api/CustomerController.java`、`api/OwnerController.java` | 参数校验、接口协议、调用幂等事务入口 |
| Service | `ordering/OrderService.java`、`checkout/PaymentService.java` | 定价快照、状态判断、加锁顺序与业务编排 |
| DAO | `persistence/dao/OrderDao.java` | 类型明确的数据库操作接口，由 MyBatis 生成代理 |
| Mapper XML | `src/main/resources/mapper/OrderDao.xml` | SQL、参数绑定、resultMap、FOR UPDATE |
| DO | `persistence/dataobject/OrderDO.java` | `customer_order` 表字段，普通 JavaBean |
| 类型转换 | `persistence/typehandler/UtcInstantTypeHandler.java` | Instant 与 UTC DATETIME(6) 的双向转换 |
| 事务配置 / 检查 | `config/PersistenceConfiguration.java`、`transaction/RequireWriteTransaction.java` | 同一 DataSource、事务管理器以及写方法的前置条件 |

十张业务表各有一个 DO、DAO 和 XML。没有再加只做转发的 Repository、通用 BaseDao 或复杂代码生成框架。业务层通过转换方法将 DO 组装为独立的不可变模型/DTO；DO 不直接返回给客户端。

## 建议先看一次支付

从 `CustomerController.simulatePayment()` 进入 `IdempotencyService.executeIdempotentRequest()`，在 `TransactionTemplate.execute()` 回调中完成以下工作：

1. `IdempotencyDao.preInsertIdempotencyRecord()` 竞争幂等唯一键，`selectByScopeAndKeyForUpdate()` 读取并校验原请求。
2. `PaymentService.processMockPayment()` 调用 `OrderService.lockOrder()`，由 `OrderDao.selectByUuidForUpdate()` 锁订单。
3. `InventoryService.deductStockAndRecordChanges()` 按内部整型商品 ID 排序，通过 `ItemInventoryDao.selectByItemIdForUpdate()` 锁定并检查全部库存。
4. `ItemInventoryDao.deductStock()` 条件扣减，`InventoryChangeLogDao.insert()` 写库存流水。
5. `OrderDao.markPaid()` 更新状态，`ReceiptDao.insert()` 写小票。
6. `IdempotencyDao.updateResponseById()` 保存完整响应；最外层事务提交后才返回 HTTP 结果。

其中任何 SQL 或业务步骤抛出异常，异常继续传播至最外层，由 TransactionTemplate 整体回滚。不要在回调中吞掉失败异常后正常返回；如果确实要捕获并继续处理，需要明确调用 `setRollbackOnly()` 或另行设计恢复流程。

## 为什么 MyBatis 不会另外提交一次

MyBatis-Spring 注入的 Mapper 代理通过 Spring 管理的 SqlSession 执行 SQL。`PersistenceConfiguration` 中的 DataSourceTransactionManager 与 MyBatis 使用同一个 DataSource，因此 DAO 操作加入 TransactionTemplate 已开启的事务。没有手动 `openSession()`、`commit()`、`rollback()`，也没有 `REQUIRES_NEW` 子事务。

写入/加锁 Service 的 public 入口使用原生 MANDATORY 的组合注解 `@RequireWriteTransaction`，并由事务管理器严格检查只读属性。它要求加入已有可写事务，**不会自行开启或提交事务**。调用必须经过注入的 Spring 代理；同类 this 调用不拦截。参与者抛出运行时异常会标记共享事务 rollback-only，外层不能吞掉异常后继续提交。

这个检查只保护现有 Service 入口。直接从新代码调用 DAO 写库，仍可能在事务外自动提交；扩展时必须继续遵守入口约定，不能把 guard 当成全局拦截器。

## MyBatis 里额外处理的边界

- **一级缓存**：配置为 `STATEMENT`，避免同一事务中的第二次查询沿用 Mapper 会话里较早的结果。二级缓存关闭。
- **加锁查询**：三个 `FOR UPDATE` 查询还显式设置 `useCache=false`、`flushCache=true`，保证执行数据库加锁语句。`useCache=false` 单独并不等于关闭一级缓存。
- **执行器**：使用 `SIMPLE`。条件扣库存和状态更新需要立即检查影响行数；切换到 `BATCH` 会改变执行/报错时点，不能直接替换。
- **映射**：XML 使用显式 resultMap，不依赖列顺序和隐式自动映射。多参数 DAO 方法使用 `@Param`，SQL 使用 `#{...}` 绑定。
- **幂等 key**：DO 中使用 `byte[]`，对应 MySQL `VARBINARY`。先校验 ASCII，再编码，大小写不会被 collation 合并。
- **时间**：用 UTC Calendar 显式读写 DATETIME(6)，保持空 `paid_at` 与微秒精度；Calendar 每次创建，避免多线程共享可变对象。
- **日志**：默认关闭 MyBatis SQL 参数日志，避免顾客姓名、幂等 key 和响应内容进入日志。需要排查时优先在 DAO 调用处打断点。

## 如何验证这次改动

运行方式仍是 `./run-local.sh`，本地测试用 `./run-tests-local.sh`。ver1.9 未改动 ver1.7 的 Liquibase changeset、表结构或业务代码。只确认 ver1.7 基线未变，不代表任意旧库都可直接升级；已有库先核对结构和迁移记录。

原有真实 HTTP/MySQL 测试继续验证支付幂等、库存竞争、失败回滚及重启恢复。现有测试还检查：绕过事务或在只读事务里调用写 Service 会被拒绝；同一 READ COMMITTED 事务内，再次调用 DAO 能读到另一连接刚提交的库存变化。时间测试还比较实际时间范围，防止两端都偏移却误判成功。

集成测试仍使用 JdbcTemplate 独立查询数据库、构造竞争和注入故障，这是对 MyBatis 结果的交叉检查；生产业务代码已全部改为 DAO 调用。具体测试数与实际执行环境见 [VERIFICATION.md](VERIFICATION.md)。

依赖固定为 MyBatis Spring Boot Starter 2.3.2（MyBatis 3.5.14 / MyBatis-Spring 2.1.2），与这份项目的 Java 8 / Boot 2.7 基线配套。基础机制参考 [MyBatis-Spring 事务文档](https://mybatis.org/spring/transactions.html) 和 [MyBatis 配置文档](https://mybatis.org/mybatis-3/configuration.html)。

## 内部与外部身份

- 十表 DO 的 id、categoryId、itemId、orderId 使用 Long；categoryCode/itemCode/offerCode/orderUuid 是独立的原对外标识。
- OrderDO.status 使用 Integer（1 待支付、2 已支付）；领域 OrderStatus 映射数据库编码，JSON 仍为枚举名称。active 仍是 Boolean。
- createTime/updateTime/paidAt 对应数据库 create_time/update_time/paid_at；HTTP createdAt/paidAt 不变。
- MenuItem、Order、OrderLine 的内部主键字段以 @JsonIgnore 排除；原 JsonCreator 构造函数保留。业务从数据库构造的实例携带内部 ID，纯 HTTP 反序列化得到的实例不用于写库/扣库存。
- OrderLineDO.itemCode 是 LEFT JOIN menu_item 得到的只读映射属性，不是订单明细存储列；关联查询不按 active 筛选，名称、价格始终来自订单快照。缺少关联身份时明确报错，不静默丢失明细。
- 库存及流水表为 item_inventory / inventory_change_log，库存数量属性为 stockQuantity；已有接口 availableStock 保持不变。
- 去掉物理外键并不意味着可以随意删除被引用的身份；本版未增加后台 CRUD。

## 命名与字段说明

Service 的依赖按类型直白命名，例如 orderDao、inventoryChangeLogDao、idempotencyDao。DO 字段以 Javadoc 标明数据库列名及含义。OrderConverter 改为静态工具类。支付时间统一为 Java paidAt / SQL paid_at。V1 是新空库初始化基线，直接定义最终字段、索引及参考数据，不包含旧版本 ALTER。已有数据库部署后，后续变化应新增迁移。
