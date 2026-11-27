# 设计说明 · ver1.9

一个 Spring Boot 应用，一个 MySQL 事务库。六个原契约接口；API 外部字符串 code / UUID 不变，内部用整型主键关联。十张表无物理外键，服务校验和唯一索引共同保证当前业务不变量。

## 1. 模块

| 包 | 职责 |
| --- | --- |
| api / api.request / api.response | 顾客/店主 Controller、请求校验、响应模型 |
| api.error | ApiExceptionHandler：异常映射 HTTP |
| common.error | 稳定业务码、业务异常、受控内部异常 |
| common.json | 共享注入的 ObjectMapper / JsonCodec |
| catalog / catalog.converter | MenuService 查询组织、MenuConverter 纯映射 |
| ordering / ordering.converter | 创建/读取订单、纯计价、静态 OrderConverter |
| checkout | 模拟支付、库存、不可变小票 |
| idempotency | 请求作用域/key、内容摘要、事务执行和响应回放 |
| persistence.dao / dataobject / mapper XML | MyBatis 接口、表记录与显式字段映射 |
| transaction / config | MANDATORY 约束、配置/鉴权/JSON/日志 |

## 2. 原子边界

IdempotencyService 的 TransactionTemplate 开启 READ_COMMITTED，依次预插入幂等记录、锁定读取、核对请求内容、执行业务、保存原响应。事务正常完成后才返回给 Controller。MyBatis 使用同一 DataSource，MANDATORY 参与外层可写事务；不建立独立业务事务。

支付先锁订单，再按内部 item ID 升序逐条锁库存；先检查所有库存，再扣减和记流水、标记 PAID、生成小票。库存条件 UPDATE 是额外保护，不把它叫成 version 乐观锁。行锁到整个事务结束才释放。排序减少相反顺序造成的锁环，不能推广为任何数据库操作都绝不会死锁。

同 key/同内容回放原 HTTP 状态和 JSON；同 key/不同内容 409；不同 key/同订单仍由订单锁和 PAID 状态避免双扣。预插入和业务同事务，异常回滚时不会留下新占位。DECLINED 是正常返回，会保存响应；相同 key 的后续请求继续得到当时的拒绝结果，新尝试使用新 key。

## 3. 价格和库存

整数最小货币单位；参考 2 杯卡布奇诺 + 1 可颂 = 1250 - 100 = 1150。multiplyExact/addExact/subtractExact 检测溢出。创建时保存商品名称/单价/数量/金额及订单总额；支付不重新计价。

菜单显示生效 1+1 优惠，后端按购物车数量匹配。不重叠是本实现选择的简化约束，不是 PRD 对未来促销的限制。复杂重叠优惠、显式套餐、最优价搜索仍为扩展。创建只检查可售库存、不预占；支付争抢库存失败返回 409，原订单仍待支付。

## 4. 配置、异常与身份

默认店铺 ID 由配置指定，非法/缺失值验证失败；启动时校验记录存在，运行读取也检查缺失，绝不回退到 1。种子 ID=1 只是默认演示数据，不表示多店支持。

内部数据缺失用 InternalErrorCode + 只接收数字 ID 的命名工厂；对外仍 INTERNAL_ERROR/500。受控异常记录代码/ID/requestId 和堆栈；第三方异常不直接记录 message/cause，只保留类型及有限堆栈位置。暂时数据库故障保留 503/Retry-After，事务编程错误不误报成可重试。

店主接口通过 Spring Security Basic + BCrypt + OWNER；演示凭据不在顾客响应中。顾客 UUID 只降低枚举便利性，不是授权设计；真实产品仍要补顾客身份与资源权限。

## 5. 状态、时间与演进

PENDING_PAYMENT + DECLINE 仍 pending；APPROVE 成功后 PAID；已 PAID 的 APPROVE 返回原业务结果，DECLINE 返回 409。Instant 表示时间点，Clock 注入利于确定性测试；数据库 DATETIME(6) 按 UTC 约定转换。paidAt 相同用稳定订单号次级排序，不宣称全局提交先后。

真实支付是后续扩展：订单状态之外需保存独立支付尝试，明确未知结果、回调重复/乱序和库存不足时的补偿策略。真实渠道扣款不能加入本地 DB 事务，需要稳定渠道请求号、回调去重/验签、主动查单与补偿；本项目未实现渠道接入。
