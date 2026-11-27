# 需求追踪与验收矩阵

更新：2026-11-27。ver1.9 的业务源码、测试及 SQL 与 ver1.7 相同。以下次数来自 ver1.7 历史验收，在 JDK8/Maven3.6.3/MySQL5.6.30 原生环境执行，71 个用例全部通过；Docker 运行未验证。具体证据见 [验证记录](VERIFICATION.md)。

## 1. 必需接口与行为

源码路径前缀：`src/main/java/com/example/coffee/`；测试路径前缀：`src/test/java/com/example/coffee/`。原文要求在 [角色作业](../reference/04_Backend_Engineer_Senior_Assignment.docx) 和 [通用规格](../reference/01_Shared_Coffee_Shop_Specification_v1_1.docx)。

| 编号 | 需求 | 当前实现入口 | 已有测试/证据 |
| --- | --- | --- | --- |
| R01 | GET menu：分类、单品、整数金额、库存、有效优惠 | api/CustomerController、catalog/MenuService | HttpApiTest.referenceFlowMatchesProvidedFixtures |
| R02 | POST orders：服务端计价、PENDING_PAYMENT | ordering/Pricing、OrderService | PricingTest；referenceFlowMatchesProvidedFixtures |
| R03 | GET order：状态、价格、付款时间 | OrderService.getOrder、OrderConverter | referenceFlowMatchesProvidedFixtures；unicodeNamesAndUtcTimestampsSurviveMySqlRoundTrip |
| R04 | POST payments/mock：确定性批准/拒绝 | checkout/PaymentService | paymentDeclineReplayAndLaterApprovalHaveExplicitSemantics |
| R05 | GET receipt：仅 PAID，含订单号/付款时间 | checkout/ReceiptService | referenceFlowMatchesProvidedFixtures；paymentDeclineReplayAndLaterApprovalHaveExplicitSemantics |
| R06 | owner queue：认证、paidAt 升序、稳定次序 | api/OwnerController、SecurityConfiguration、OrderDao.xml | ownerAuthenticationOrderingAndStatusFilter |
| R07 | 输入校验，不留下无效订单 | request 注解、JsonConfiguration、Pricing | invalidInputDoesNotPersist；strictJsonRejectsMalformedOrAmbiguousRequests；unsupportedAndDuplicateItemsAreRejected |
| R08 | 同 key 同内容一次创建并回放 | IdempotencyService、IdempotencyDao.xml | concurrentSameKeyCreateProducesOneOrderAndIdenticalResponses；createReplayIsOriginalEvenAfterPaymentAndCanonicalizesOrder |
| R09 | 同 key 不同内容冲突，原数据保留 | 指纹比较+唯一键 | concurrentConflictingPayloadsChooseOneWinner |
| R10 | 不同 key 支付同单不双扣 | 订单 FOR UPDATE+PAID 分支 | concurrentPaymentsWithDifferentKeysStillDeductExactlyOnce |
| R11 | 库存充足才批准，争抢最后一件 | InventoryService、ItemInventoryDao.xml | lastItemRaceAllowsExactlyOnePaidOrder |
| R12 | 多商品扣减不能部分成功 | 同事务、先锁定检查全部库存 | multiItemInsufficientStockDoesNotPartiallyDeductAndCanBeRetried |
| R13 | 后续写入失败整体回滚 | 小票与幂等结果同事务 | failureAfterDeductionRollsBackOrderStockReceiptAndKeyTogether |
| R14 | 价格快照不受后续目录变化 | order_line 与主表金额快照、receipt payload | existingPriceAndReceiptDoNotChangeWhenCatalogueChanges |
| R15 | 订单/库存持久化 | MySQL InnoDB，Liquibase V1 | PersistenceRestartTest；ver1.7 验收数据库及应用实际重启断言 |
| R16 | 认证失败不泄漏内部信息 | SecurityConfiguration、ApiExceptionHandler | ApiErrorMappingTest；ownerAuthenticationOrderingAndStatusFilter |
| R17 | 事务误用不能悄悄工作 | RequireWriteTransaction+事务管理器 | writeServicesRejectMissingOrReadOnlyTransaction；caughtMandatoryFailureStillRollsBackTheOuterTransaction |
| R18 | 锁超时可恢复，无部分副作用 | DatabaseFailureClassifier、外层回滚 | boundedLockWaitReturnsRetryableErrorAndLeavesNoPartialWrites |
| R19 | SQL 与当前 MySQL 版本兼容 | V1__schema.sql、init-local.sql | ver1.7 验收 MySQL5.6.30 自动初始化及 init-local.sql 两次执行通过 |
| R20 | 可复现启动、种子、健康、示例 | run-local.sh、Dockerfile/compose、demo、actuator | ver1.7 验收原生 JAR/HTTP/英文 demo 通过；Docker 仅静态检查通过 |

## 2. 测试分层

| 测试类 | 类型 | ver1.7 验收执行次数 | 证明范围 |
| --- | --- | ---: | --- |
| PricingTest | 纯计价单元测试 | 9 | 参考定价、边界、溢出、重叠拒绝 |
| ApiErrorMappingTest | Web 层/错误映射定向测试 | 14 | HTTP/code/响应保密、分类逻辑；不能证明真实 DB 锁 |
| HttpApiTest | 应用 HTTP + 真实 MySQL 集成测试 | 40 | 事务、认证、输入、并发、快照等 |
| PersistenceRestartTest | 应用重启集成测试 | 1 | 已提交数据与幂等记录在应用重启后仍可读 |
| ShopConfigurationTest | 配置/启动校验定向测试 | 4 | 绑定非默认 ID、缺失/非正值拒绝、不存在的店铺拒绝 |
| InternalDiagnosticsTest | 转换/日志/HTTP 定向测试 | 3 | 区分内部码、稳定外部 500、日志保密与受控堆栈 |

参数化测试执行次数不等于方法数。ver1.7 验收合计 71，失败/错误/跳过均为 0。旧版本结果不替代本条记录。

## 3. 验收分层

- 实际原生：Zulu JDK1.8.0_514、Maven3.6.3、Linux AMD64、MySQL5.6.30 Community Server (GPL)，JVM Asia/Shanghai。
- 初始化：init-local.sql 重复执行成功；独立空业务库启动执行唯一 V1；重启不重插种子。
- 运行：JAR 启动、HTTP 有断言流程、数据库及应用停止后重启、英文 demo 全部通过。
- 配置：不存在的默认店铺在实际 JAR 启动时失败，含 SHOP_CONFIGURATION_MISSING。
- 图形：两份 PlantUML 已生成 SVG。
- Docker 静态：Compose v2.32.4 config、官方 schema、Shell 语法通过。
- **未执行：Docker build、容器启动/停止/重建、挂载持久化、备用 Testcontainers 路径。** 当前无 Docker Engine，提交前执行 scripts/docker-acceptance.sh 补齐。

## 4. 本版新增检查

默认店铺与受控内部日志测试已纳入 71 用例；纯改名/分包通过编译和已有 HTTP/数据库回归覆盖。自动 Docker 验收独立创建两个库，完成后保留结果目录与数据，不影响本地业务库。

## 5. 原范围以外

顾客/店主 UI、菜单 CRUD、复杂促销、日统计、真实支付、税、多店、事件总线和微服务不是本高级后端切片必需项。文档里的扩展方案不是已实现或验收的能力。
