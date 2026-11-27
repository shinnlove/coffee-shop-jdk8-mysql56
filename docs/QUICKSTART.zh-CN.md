# Intel Mac 本地运行与 IDEA 调试

本文在实现分支、含 `pom.xml` 的项目根目录执行。目标：Java8、Maven3.6.3、MySQL5.6.30/InnoDB。Docker 路径见 [Docker 手册](DOCKER.zh-CN.md)。

## 1. 核对版本

```bash
java -version
mvn -version
mysql -u root -p -e 'SELECT VERSION(), @@version_comment;'
```

Mac 有多个 JDK 时可用 `/usr/libexec/java_home -V` 查看，再配置 IDEA 的 Project SDK、Run Configuration JRE、Maven Runner JRE；三处可能不同。项目自带 `mvnw`、`mvnw.cmd` 和 `.mvn/wrapper/maven-wrapper.properties`，Wrapper 固定 Maven3.6.3。首次运行会下载 Maven；本机仍需 JDK8，Wrapper 不安装 JDK。不要遗漏隐藏的 `.mvn` 目录。

`run-local.sh`、`run-tests-local.sh` 优先使用 PATH 中的 `mvn`，找不到才使用 Wrapper，所以仍需检查系统 Maven 版本。若要强制固定 3.6.3，直接运行本文的 `./mvnw` 命令。

## 2. 新空库

```bash
mysql -u root -p < scripts/init-local.sql
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

账号初始化 SQL 兼容 5.6.30，可重复执行；已有账号不会被改密码。业务数据库 coffee_shop、测试数据库 coffee_shop_test。应用首次启动时 Liquibase 自动执行唯一 V1，建十张 InnoDB 表和参考数据；没有 V2/V3，也无需手工执行 V1。默认应用用户 coffee_app / coffee-local-only，测试用户 coffee_test / coffee-test-only，仅供本机。

## 3. IDEA

CoffeeApplication 的 Active profiles 填 `local`。公共配置声明 `spring.profiles.group.local: demo`，所以启用 local 同时启用 demo，不是启动三个应用。配置叠加为公共 + demo + local 中适用的属性。

命令行可等价使用：

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
# 或先构建 JAR（这里只构建，不代替完整测试）
./mvnw -DskipTests package
java -jar target/coffee-shop-1.0.0.jar --spring.profiles.active=local
```

DB_URL、DB_USERNAME、DB_PASSWORD、DEFAULT_SHOP_ID 可以在 IDEA Environment variables 中设置。程序参数用 `--spring.profiles.active=local`；JVM 参数则为 `-Dspring.profiles.active=local`，不要把两者放错输入框。默认 local 监听回环地址 127.0.0.1。

## 4. 已经有表的本地环境

如果手工建表后遇到 Table shop already exists，说明业务表已存在，但不代表 DATABASECHANGELOG 已经同步。正确完整属性是 `spring.liquibase.enabled=false`（YAML 嵌套在 spring 下）；顶层 `liquibase.enabled` 不能当成 Boot2.7 的正确配置。

如需暂时继续使用已核对结构的旧库，在本机 Run Configuration 添加 `--spring.liquibase.enabled=false`，不要修改公共基线让评审的新空库也跳过初始化。若准备重新验证自动建表，创建一个独立的新库并授予专用账号权限，再通过 DB_URL 指向新库。不要为了消除报错删除你已有的数据。

真正长期沿用现有库时，需要核对 schema 和 changeset 后再选择迁移基线同步方式，不能随便伪造 DATABASECHANGELOG；后续 Git 管理和部署后用追加迁移。

## 5. 冒烟和完整测试

```bash
curl -fsS http://localhost:8080/actuator/health
./scripts/demo.sh
./run-tests-local.sh
```

demo 是五步可读演示，会扣库存；店主 Basic 演示账号为 `owner / coffee-demo-2026`，仅用于本地。HTTP 断言脚本可用：

```bash
python3 scripts/acceptance_http.py create --state .local/http-state.json
python3 scripts/acceptance_http.py verify --state .local/http-state.json
```

两步之间如运行其他扣库存操作，精确库存断言可能失败；建议使用独立演示库。自动化测试重置 *_test 库，用例包括并发、回滚和应用重启；禁止把 TEST_DB_URL 指向业务库。

需要 JVM 远程调试可用 `./run-debug.sh`，默认回环 5005；或在 IDEA 直接点 Debug。健康不通过先看数据库连接、店铺配置和 migration 日志，接口有 4xx 不等于服务器崩溃。

## 6. 固定 Maven 的测试命令

纯定价测试不依赖正在运行的应用或数据库：

```bash
./mvnw -Dtest=PricingTest test
```

先执行第 2 节账号初始化，再连接专用测试库运行完整测试：

```bash
export TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/coffee_shop_test?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8'
export TEST_DB_USERNAME=coffee_test
export TEST_DB_PASSWORD=coffee-test-only
./mvnw -B -Duser.timezone=Asia/Shanghai clean verify
```

测试会清理该测试库；不要使用业务库。未设置 TEST_DB_URL 时，直接 verify 会走 Testcontainers，需要 Docker 和预先构建的数据库镜像，见 Docker 手册。`-DskipTests package` 只用于生成本地运行产物，不能计为测试通过。

## 7. 接口调试

IDEA Swagger、Apifox 或 Postman 导入项目根目录 `openapi.yaml`。契约描述请求格式；完整用例还需要实际数据、调用顺序、变量提取和断言。`reference/reference_fixtures.json` 是样例数据，不是直接可运行的请求集合。

建议依次执行：菜单 → 创建订单（保存 id）→ 查询订单 → DECLINE → 换支付 key APPROVE → 查询小票 → 店主队列。同一次请求重试复用 key 与请求内容；不同支付尝试使用新 key。可从 scripts/demo.sh 复制每个 cURL；无需把整个 Collection 存成 cURL 文本。

并发争抢、失败回滚和持久化重启以 Java 集成测试为准，单次手工调用不替代这些断言。
