# 验证记录 · ver1.9

日期：2026-11-27（Asia/Shanghai）。历史业务验收与本版分发检查分别列出。**Docker 实机镜像构建、容器启动/停止/重建及目录挂载持久化仍为 NOT RUN。**

## 1. ver1.7 的业务验收证据

下面是保留的 ver1.7 实际执行结果，不是声称本次重新跑完了全套业务验收。

| 环境 / 项目 | 历史结果 |
| --- | --- |
| 执行环境 | Linux x86_64，非 Intel Mac |
| JDK | Zulu OpenJDK 1.8.0_514 |
| Maven | Apache Maven 3.6.3 |
| MySQL | 5.6.30 / MySQL Community Server (GPL)，InnoDB |
| JVM 测试时区 | Asia/Shanghai；应用与数据库按 UTC 转换 |
| 全部必要测试 | 71 执行，0 失败、0 错误、0 跳过 |
| 初始化 | MySQL5.6.30 上 init-local.sql 重复执行、新空库 Liquibase V1 通过 |
| 原生运行 | 可执行 JAR、HTTP 断言、英文 demo、数据库与应用重启持久化通过 |
| Compose | v2.32.4 config 静态检查通过；无可用 Docker Engine |

测试分层：PricingTest 9、ApiErrorMappingTest 14、ShopConfigurationTest 4、InternalDiagnosticsTest 3，共 30 个定向用例；HttpApiTest 40、PersistenceRestartTest 1，共 41 个真实 MySQL 集成执行用例。

证据：[结构化结果](verification/ver1.7/acceptance-result.json)、[测试摘要](verification/ver1.7/test-summary.txt)、[JUnit 报告目录](verification/ver1.7/junit/)。JUnit XML 保留测试名称与结果，剔除机器环境属性和重复日志。JSON 中旧 JAR 的 SHA-256 是历史构建证据，不表示当前仓库附带该二进制文件。

## 2. ver1.9 的变更与检查

本版调整 README、图示、公开/本地资料分发、失效引用、运行说明和 Compose 发布标签，补齐 Wrapper 交付。业务 Java、全部测试、SQL、MyBatis XML、配置资源、pom.xml、实际 OpenAPI 与 ver1.7 按字节比较；结果见 [本版检查报告](verification/ver1.9/packaging-checks.json)。

源码摘要算法：按相对路径排序 `src/` 下全部文件，再加 `pom.xml`、`openapi.yaml` 一并排序；逐项向 SHA-256 输入“UTF-8 相对路径 + NUL + 文件内容”。与 ver1.7 结构化证据比较，防止把不同业务源码的历史结果当作本版依据。

本版单独检查两个 ZIP 的共有文件一致、master 资源完整、文档本地链接、隐藏 Wrapper 文件与执行权限，以及模拟 master 初始化后覆盖完整包、普通 git add 不纳入本地资料。检查记录不等同于重新执行数据库并发测试或 Docker 生命周期。

## 3. 仍需补齐的环境验收

- Dockerfile 实际构建，包括网络下载、镜像层和入口脚本运行。
- app/db 容器启动、停止/启动、删除容器后重建。
- 实际目录挂载持久化、容器信号、权限和健康检查。
- Docker 测试容器与备用 Testcontainers 路径。
- 本次 ver1.9 完整包在目标 Intel Mac 的实际运行。

运行 [Docker 手册](DOCKER.zh-CN.md) 的 `./scripts/docker-acceptance.sh`。只有脚本最终 PASS、退出码 0 且报告检查通过，才能更新容器验收状态。运行输出默认被 Git 忽略，公开前应审阅并整理新证据；不能改写 ver1.7 历史记录来表示本次通过。
