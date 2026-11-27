# AI 辅助与第三方资源使用说明

本项目使用 AI 协助理解需求、讨论架构与边界、生成和修改 Java/Spring Boot 代码、设计测试、整理文档及执行验证。候选人负责需求选择、代码审阅、结果确认，并对提交内容及其技术解释负责。

关键取舍包括数据库幂等与业务共用事务、付款时检查并扣减库存、内部自增 ID 与外部契约标识分离，以及按作业规模采用不重叠的 1+1 促销。复杂促销、真实支付和多店能力没有列为已实现。

依赖包括 Spring Boot/Security、MyBatis、MySQL Connector/J、Liquibase、JUnit、AssertJ、Mockito、Testcontainers；精确版本以 [pom.xml](../pom.xml) 为准。Maven Wrapper 使用 Apache Maven 的脚本，固定 Maven 3.6.3；脚本内保留上游许可说明。图示用 PlantUML 生成，源码可查看和修改。

MySQL 容器构建从官方归档下载 5.6.30 二进制包，并核对固定 SHA-256。项目不附带真实支付凭据或外部顾客数据。演示凭据公开，仅用于本地测试。

四份题目资料保存在 [reference](../reference/README.md)，保留原始字节。实际测试环境、历史证据、本版文档检查及尚未运行的 Docker 路径见 [验证记录](VERIFICATION.md)。

官方资料：[Spring Boot 2.7.18](https://docs.spring.io/spring-boot/docs/2.7.18/reference/html/)、[MyBatis-Spring 事务](https://mybatis.org/spring/transactions.html)、[Maven Wrapper](https://maven.apache.org/wrapper/)、[PlantUML](https://plantuml.com/)、[MySQL 归档](https://downloads.mysql.com/archives/community/)。
