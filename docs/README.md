# 项目文档

本目录位于实现分支。命令均在含 `pom.xml` 的项目根目录执行。

| 阅读目的 | 文档 |
| --- | --- |
| 本地运行、IDEA、Profiles、测试 | [本地运行手册](QUICKSTART.zh-CN.md) |
| 构建容器与验证持久化 | [Docker 使用手册](DOCKER.zh-CN.md) |
| 事务、幂等、库存、促销取舍 | [设计说明](DESIGN.md) |
| 原要求对应的实现与测试 | [需求追踪与验收矩阵](REQUIREMENTS.md) |
| 已执行与未执行的验证 | [验证记录](VERIFICATION.md) |
| SQL 映射与事务参与方式 | [MyBatis 说明](MYBATIS.zh-CN.md) |
| 时间字段与 UTC 转换 | [Instant 时间模型](TIME_MODEL.zh-CN.md) |
| 版本适配 | [JDK8 / MySQL5.6.30](JDK8_MYSQL_NOTES.zh-CN.md) |
| AI 和第三方依赖 | [使用说明](AI_USAGE.md) |
| 实际 API | [openapi.yaml](../openapi.yaml) |
| 题目原件 | [四份参考文件](../reference/README.md) |

图示：[支付概览](diagrams/coffee_shop_overview.svg)、[ER](diagrams/coffee_shop_er.svg)、[完整时序](diagrams/coffee_shop_business_sequence.svg)。对应 PlantUML 源文件与图放在同一目录。

ver1.9 是分发与文档整理版本。原业务源码、测试、SQL 和实际 API 契约保持不变；历史验收和本版检查分别记录，不将未执行的 Docker 运行写为通过。
