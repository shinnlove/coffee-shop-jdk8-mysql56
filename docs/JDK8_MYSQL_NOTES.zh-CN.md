# JDK8 / MySQL5.6.30 兼容说明

当前约定：JDK8、Maven3.6.3、Spring Boot2.7.18、MyBatis2.3.2、MySQL Community Server5.6.30 / InnoDB。精确依赖版本见 pom.xml，实际环境和测试见 [VERIFICATION](VERIFICATION.md)。

MySQL5.6 不使用 CREATE USER IF NOT EXISTS；账号脚本先检查，再通过 prepared statement 创建或空操作。DATETIME(6)、CURRENT_TIMESTAMP(6)、InnoDB 唯一索引及条件更新在目标版本上实际跑过。唯一 V1 同时创建最终结构和种子，无 V2/V3 ALTER。已有部署历史后再用追加迁移，不覆盖 V1。

应用配置 UTC 连接和显式 Instant TypeHandler；Java8 不支持新语言语法，实际编译由 JDK8 完成。金额字段沿用 *Minor。Docker 的旧 MySQL 标签因清单格式问题替换为官方二进制归档构建，版本未改变；镜像本身仍待实机验收。

启动和迁移步骤以 [本地说明](QUICKSTART.zh-CN.md) / [Docker 手册](DOCKER.zh-CN.md) 为准。
