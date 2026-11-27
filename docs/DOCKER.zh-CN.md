# 本项目 Docker 使用手册 · ver1.9

适用设备：Intel Mac（x86_64）；目标版本：JDK 8、Maven 3.6.3、MySQL Community Server 5.6.30。下列命令都在项目根目录执行。

**验证边界：** ver1.7 的原生 JDK8 + Maven3.6.3 + MySQL5.6.30 已通过完整必要测试、JAR 启动和数据库/应用重启持久化验证。ver1.9 仅调整文档与分发结构，并沿用这些历史证据；本版也没有可用 Docker Engine，镜像构建和容器生命周期尚未实跑。Compose 配置、Shell 语法及官方镜像清单已检查，但不能替代容器验收。运行第 8 节的一键脚本补齐证据后，才能勾选 Docker 交付项。

## 1. 先理解本项目怎么打包

不是把 Spring Boot 和 MySQL 塞进同一个容器。`Dockerfile` 构建应用，`docker/mysql/Dockerfile` 构建指定版本的数据库；Compose 创建网络、启动顺序、端口和挂载目录。

| 对象 | 本项目用途 |
| --- | --- |
| 应用 build 阶段 | 固定 Maven 3.6.3 + JDK8，编译、单元/定向测试、生成可执行 JAR |
| 应用 verification 阶段 | 保留构建环境，连接独立 test-db 运行完整 `mvn verify` |
| 应用 runtime 阶段 | Java8 JRE + JAR；内置 Tomcat，无需另装外部 Tomcat；非 root 运行 |
| 数据库镜像 | Ubuntu 22.04 + Oracle 官方 MySQL 5.6.30 二进制包；SHA-256 校验 |
| compose.yaml | 日常演示 app + db |
| compose.test.yaml | 追加独立 test-db + tests；通过 test Profile 启用 |

为什么自建 MySQL 镜像：2026-11-27 查询注册表，`mysql:5.6.30` 仍返回 Schema 1 清单。Docker 官方已移除此格式支持，直接引用旧标签可能无法拉取。因此从官方归档构建，数据库版本仍为 5.6.30，不暗中替换为 5.7/8.0。官方包约 300 MiB，下载后须匹配 Dockerfile 中固定校验值。

MySQL 初始化由入口脚本完成：建系统表 → 仅本机临时 socket 启动 → 创建应用库/账号 → 设置本地 root 密码 → 停临时实例 → 启动正常实例。重启不会重建账号或种子。发现有系统表却无本项目初始化成功标记时明确退出，避免把半初始化目录当成功；会保留目录供检查。

## 2. 环境检查

先启动 Docker Desktop，等待 Engine 就绪：

```bash
uname -m
docker version
docker compose version
docker info
python3 --version
curl --version
```

Intel Mac 应为 x86_64；Compose 使用 v2 命令 `docker compose`。Docker 客户端存在但 Server 无法连接，不能算环境就绪。运行 Docker 路径不依赖宿主机 JDK/Maven，构建容器内固定为 8/3.6.3；本地 IDEA 路径仍需自己核对。

```bash
java -version
mvn -version
./mvnw -version
mysql -u root -p -e 'SELECT VERSION(), @@version_comment;'
```

若 Mac 端口 8080 已被 IDEA 占用，先停 IDEA，或使用 `APP_PORT=18080 docker compose up --build -d`。数据库默认发布 3307，不占你原有本机 3306。

## 3. 文件和持久化目录

| 宿主机位置 | 容器位置 / 目的 |
| --- | --- |
| docker/data/mysql/ | db 的 /var/lib/mysql；业务表、系统表、迁移记录、订单等 |
| docker/data/mysql-test/ | test-db 的 /var/lib/mysql；自动化测试专用 |
| docker/results/tests/ | tests 的 /results；保存 JUnit 报告 |
| docker/data/acceptance-运行编号/ | 一键验收独立业务库，避免影响日常数据 |
| docker/data/tests-运行编号/ | 一键验收独立测试库 |
| docker/results/运行编号/ | 构建日志、测试、版本、容器日志、HTTP 断言与退出码 |

这里使用 **bind mount（宿主机目录挂载）**，数据库写入宿主机指定目录，随容器替换保留。目录随源码提供占位文件，实际数据被 Git 和构建上下文忽略。不要把运行中的 MySQL 数据目录打入 ZIP 或 Git，也不要让两个 MySQL 实例同时挂载同一目录。

应用只保存业务数据到数据库，日志写标准输出；Docker 采用 json-file 轮转，每份 10 MiB、最多三份。需要可见日志文件时执行第 6 节导出命令，避免额外制造容器文件权限问题。

## 4. 首次构建并启动

```bash
# 可选：想改端口/目录时复制，再编辑 .env。
cp .env.example .env

# 校验配置，不打印密码。
docker compose config --quiet

# 第一次会构建两个镜像；-d 表示后台运行。
docker compose up --build -d

docker compose ps
# 等 app 状态为 healthy 后执行；启动期间连接失败不代表最终启动失败。
curl -fsS http://localhost:8080/actuator/health
./scripts/demo.sh
```

健康检查最终应返回 `{"status":"UP"}`。第一次可能需较久：下载镜像和 MySQL 包 → 编译 → 初始化数据库 → Liquibase 建表和种子 → 店铺配置检查 → 应用健康。

容器内应用连接 `db:3306`；宿主机客户端连接 `127.0.0.1:3307`。容器里的 localhost 指它自己，所以不能给应用配置 localhost 来访问另一个数据库容器。

日常 Compose 用 demo Profile；不能启用 local，否则其回环监听等本机设置不适合端口映射。公共 application.yml 始终加载，demo 叠加演示凭据。应用默认店铺取 `DEFAULT_SHOP_ID`，不是代码硬编码。

新挂载目录无需手工运行 init-local.sql 或 V1：数据库入口只建库/账号，应用 Liquibase 负责唯一 V1 建业务表和种子。已有目录不重复初始化。

## 5. 停止、再启动、重建

```bash
# 停止容器，保留容器和挂载数据。
docker compose stop app db

# 先启动数据库，观察健康，再启动应用。
docker compose start db
# 重复查看，确认 db 为 healthy 后再启动 app。
docker compose ps
docker compose start app
# 等 app 为 healthy 后再请求健康接口。
curl -fsS http://localhost:8080/actuator/health

# 删除容器和网络；宿主机挂载的数据目录仍保留。
docker compose down

docker compose up -d
curl -fsS http://localhost:8080/actuator/health

# 修改 Java 后重建应用镜像，再替换容器。
docker compose build app
docker compose up -d app
```

`start` 使用已存在的容器，不应用 Dockerfile 的新内容；`up --build` 才会构建并按配置更新容器。`restart` 不等于重建，也不保证等待数据库健康；涉及两个服务一起启动时优先让 Compose 的依赖健康条件参与，或显式等数据库健康后再启动 app。

此项目为 bind mount；`down -v` 也不是“清空这套数据库”的正确指令。任何删除宿主机数据目录都会丢失订单、库存和幂等记录。需要干净演示环境时，最稳妥的是停止旧容器并切换到一个新的空目录，保留旧目录，不直接删库。

```bash
docker compose down
fresh_data_dir="./docker/data/fresh-demo-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$fresh_data_dir"
MYSQL_DATA_DIR="$fresh_data_dir" docker compose up -d
```

这次启动后继续管理该环境时，应保持同一个 MYSQL_DATA_DIR，建议写入 .env。不要边运行边改目录。

## 6. 日志、版本、进入容器与备份

```bash
docker compose logs -f --tail=100 app
docker compose logs --tail=100 db
mkdir -p docker/results/manual
docker compose logs --no-color > docker/results/manual/containers.log

docker compose exec app java -version
docker compose exec db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql --protocol=tcp -h127.0.0.1 -u"$MYSQL_USER" -e "SELECT VERSION(), @@version_comment"'
```

构建日志和 tests 容器会打印 Maven 3.6.3。运行镜像只有 JRE，因此不要在 app 容器里找 Maven。

逻辑备份示例（先确保没有并发 DDL）：

```bash
docker compose exec -T db sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump --protocol=tcp -h127.0.0.1 -u"$MYSQL_USER" --single-transaction --routines --triggers "$MYSQL_DATABASE"' > docker/results/manual/coffee_shop.sql
```

不要把运行中数据目录简单复制当作一致性备份。恢复应在明确目标的独立库/停写窗口进行，先验证备份可用；此处不提供默认覆盖现有库的命令。

修改 `.env` 的 MYSQL_ROOT_PASSWORD / DB_PASSWORD 不会改变已有目录中账号的密码；入口只在首次初始化时使用这些值。需要主动修改数据库账号，再同步应用配置。

## 7. 完整测试与本地调试混用

独立测试容器：

```bash
mkdir -p docker/data/mysql-test docker/results/tests
docker compose -f compose.yaml -f compose.test.yaml --profile test build db tests
docker compose -f compose.yaml -f compose.test.yaml --profile test run --rm tests
```

测试只连接 test-db 的 `coffee_shop_test`，测试会清理这个专用库。预期 71 个执行用例，0 failure/error/skipped；实际次数以当前源码报告为准。单元/MVC 测试不证明真实锁，HttpApiTest 和 PersistenceRestartTest 连接真实 MySQL 才验证事务与并发。

如果只想 Docker 跑数据库、IDEA 调试 Java：

```bash
docker compose up -d db
```

IDEA Active profiles 填 local，环境变量设置：

```text
DB_URL=jdbc:mysql://127.0.0.1:3307/coffee_shop?useSSL=false&serverTimezone=UTC&characterEncoding=UTF-8
DB_USERNAME=coffee_app
DB_PASSWORD=coffee-local-only
```

密码应与你的 .env 一致。不要同时再启动 Compose app 占用 8080。日常业务库不是测试库；原生自动化测试继续用 run-tests-local.sh 配置的专用本机测试库。

不设置 TEST_DB_URL 直接 `mvn verify` 时会进入 Testcontainers 路径；它使用本项目构建的 `coffee-shop-mysql:5.6.30` 镜像，须先 `docker compose build db`。不要换回旧 Schema 1 镜像标签。该备用 Testcontainers 启动路径本次同样未实跑。

## 8. 提交前的一键 Docker 验收

```bash
./scripts/docker-acceptance.sh
```

脚本严格遇错退出，依次完成：

1. Docker Engine、Compose、curl、Python3 前置检查。
2. 创建独立项目和两个新的挂载目录；默认使用宿主机 18080/13307。
3. 构建 app、db、tests；构建阶段的定向测试必须通过。
4. 独立 test-db 上执行全部 `mvn verify`，保存 JUnit 报告。
5. 启动 app/db，等待数据库可认证查询和应用健康，读取真实 Java/MySQL/Maven 版本。
6. 创建参考订单、先拒绝后批准；验证未付款小票拒绝、鉴权、内容冲突、价格和扣库存。
7. 用同 key 回放、用新 key 再支付；检查无重复副作用。
8. 停止并启动原容器，再检查订单、小票、库存和原响应。
9. down 删除容器，再用相同挂载目录 up，重复检查持久化。
10. 保存日志、版本、退出码并停止验收容器；保留目录和证据，不删除数据。

成功必须同时看到最终 PASS 和 `exit-code.txt` 为 0；只看到部分接口输出不够。脚本生成的状态文件保存本次演示订单，用于后续阶段比较，不是生产凭据。端口冲突时：

```bash
DOCKER_ACCEPTANCE_APP_PORT=28080 DOCKER_ACCEPTANCE_MYSQL_PORT=23307 ./scripts/docker-acceptance.sh
```

每次验收使用新目录，会占磁盘。确认不再需要后再自行归档/清理。未执行脚本前，不得在业务方演示或面试中声称 Docker 生命周期已通过。

## 9. 常见问题

| 现象 | 优先检查 |
| --- | --- |
| Cannot connect to Docker daemon | Docker Desktop 是否就绪；`docker info` 是否能访问 Server |
| image manifest schema 1 | 是否误用旧 mysql:5.6.30；应构建本项目 docker/mysql/Dockerfile |
| 下载/校验失败 | 检查网络与代理、下载地址；不要删掉 SHA-256 检查绕过问题 |
| db 启动后 authentication failed | 挂载目录是否已有账号、.env 是否改了旧密码 |
| permission denied | Docker Desktop 对项目目录的文件共享权限；不要使用无必要的全局 chmod 777 |
| Incomplete or foreign data directory | 首次初始化曾中断或挂载了其他库；检查日志，换新空目录重试，保留旧目录 |
| app 连接 localhost 失败 | 容器应使用 db:3306；宿主机访问才使用 127.0.0.1:3307 |
| shop already exists | 手工建表后又启用 Liquibase，缺少迁移历史；先核对，勿直接删库 |
| SHOP_CONFIGURATION_MISSING | DEFAULT_SHOP_ID 不存在；当前单店种子 ID 为 1，不能静默回退 |
| demo 库存不足 | 多次 demo 已真实消费库存；用独立新目录演示，或按业务流程补货 |

## 10. 交付范围

这套版本用于题目演示和用户指定的兼容环境，保留 Java8/MySQL5.6。演示凭据公开、真实支付未接入、没有多租户/多店隔离。交付测试通过不等于这些老版本和简化认证适合直接生产部署。升级与真实支付需要独立设计、迁移和验证；当前范围见 [设计说明](DESIGN.md)。

官方参考：[Compose 启动顺序](https://docs.docker.com/compose/how-tos/startup-order/)、[bind mounts](https://docs.docker.com/engine/storage/bind-mounts/)、[旧清单格式移除](https://docs.docker.com/engine/deprecated/)、[MySQL 官方归档](https://downloads.mysql.com/archives/community/)。

## 11. 保存新的验收证据

`docker/results/` 是被 Git 忽略的运行输出，不能只运行 `git add .` 就认为报告已提交。完成验收后，检查并脱敏日志，在 `docs/verification/` 下新建明确版本/日期的证据目录，保存结构化摘要、测试报告、版本与退出码，再更新 [验证记录](VERIFICATION.md)。无需把数据库文件或所有原始日志公开，也不要用强制添加整个 results 目录代替审阅。

本版应用镜像标签是 `coffee-shop-app:1.9`，测试镜像是 `coffee-shop-tests:1.9`；数据库镜像仍为 `coffee-shop-mysql:5.6.30`。POM 版本保持 1.0.0，因此 JAR 文件名仍为 `coffee-shop-1.0.0.jar`。镜像标签、分发版本和 Maven artifact 版本是不同概念。
