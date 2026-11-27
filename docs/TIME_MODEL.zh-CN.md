# paidAt 为什么使用 Instant

`java.time.Instant` 是 Java 8 标准时间 API 的类型，表示时间轴上的一个确定时刻。它不可变、线程安全，内部用“从 1970-01-01T00:00:00Z 起的秒数 + 秒内纳秒”表示时刻。它不携带北京、伦敦等地区时区；展示本地时间时再指定 ZoneId。

例如 `2026-11-26T08:30:00Z` 表示 UTC 08:30，同一时刻在上海显示为 16:30。它不是“当前地区的日期时间”类型，也不是数据库专属类型。

| 比较点 | Instant | Long timestamp |
|---|---|---|
| 语义 | 类型直接说明这是一个时刻 | 是数字，需要约定它代表什么 |
| 单位 | API 明确区分秒、毫秒、纳秒 | 字段/协议需要明确秒或毫秒等单位 |
| 精度 | 可表示纳秒；实际时钟和存储精度另算 | 取决于约定，常用毫秒，也可以约定微秒 |
| 时间运算 | isBefore、plusSeconds、Duration.between 等 | 手动算整数并处理单位、溢出等 |
| 本项目 JSON | Jackson 输出契约要求的 date-time 字符串 | 默认会输出数字；需专门转换才能保持原契约 |
| 存储/分配开销 | 对象，比 primitive long 更复杂 | primitive long 简单；Long 本身仍是包装对象 |
| 适用习惯 | 领域模型、事件时间、支付成功时间 | 已明确采用 epoch 毫秒的接口或存储协议 |

两者都能正确表示同一时刻，也都不天然带地区时区。这里保留 Instant，是为了时间语义、UTC 转换和现有契约，不是因为 Long 做不到。题目约束的是 HTTP 的 date-time 格式，没有强制 Java 字段必须是 Instant。

```java
Instant paidAt = Instant.parse("2026-11-26T08:30:00.123456Z");
long paidAtEpochMillis = paidAt.toEpochMilli();
Instant restored = Instant.ofEpochMilli(paidAtEpochMillis);
// restored 为 ...00.123Z；原来的 456 微秒不能从毫秒数恢复。
```

项目的处理链路：

1. PaymentService 从注入的 Clock 取时间，截取到微秒。
2. Java 属性 `paidAt` 使用 Instant；MyBatis 的 UtcInstantTypeHandler 用显式 UTC Calendar 读写 `customer_order.paid_at DATETIME(6)`。
3. DATETIME 自身不保存时区；UTC 是本项目的统一写入/读取约定。SQL 客户端显示的是 UTC 值。
4. Jackson 使用 ISO-8601 UTC 字符串返回 `paidAt`，符合 OpenAPI 的 string/date-time；待付款订单的值为 null。
5. Receipt 保存付款成功时的快照，不在之后重新生成支付时间。

`Instant` 支持纳秒，不表示 `Clock.systemUTC()` 在每台机器上都能提供纳秒精度；数据库这里只保留 6 位小数。Java 8 的实际系统时钟通常只有毫秒精度。支付时间相同仍按契约规定的稳定次级键排序，不能靠提高类型精度消除并发同时间问题。

Java 8 API： https://docs.oracle.com/javase/8/docs/api/java/time/Instant.html
