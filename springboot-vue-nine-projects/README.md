# SpringBoot + Vue 九个独立毕业设计项目

该压缩包包含 9 个可分别打开、分别安装依赖、分别启动和部署的项目。它们不共享运行时模块；“通用后端”只作为设计基线复制到每个项目中，因此删除任意其他项目不会影响当前项目。

| # | 目录 | 项目 | 后端端口 | 前端端口 | 视觉布局 |
|---|---|---|---:|---:|---|
| 1 | `01-campus-club` | 高校社团管理系统 | 8101 | 5101 | sidebar |
| 2 | `02-snack-groupbuy` | 校园零食团购平台 | 8102 | 5102 | store |
| 3 | `03-rental-platform` | 房屋出租信息发布平台 | 8103 | 5103 | editorial |
| 4 | `04-homework-management` | 课程作业提交管理系统 | 8104 | 5104 | notebook |
| 5 | `05-lost-found` | 校园失物招领平台 | 8105 | 5105 | split |
| 6 | `06-gym-membership` | 健身房会员管理系统 | 8106 | 5106 | dark |
| 7 | `07-flower-shop` | 花卉商品在线销售管理系统 | 8107 | 5107 | boutique |
| 8 | `08-online-library` | 线上图书借阅管理平台 | 8108 | 5108 | classic |
| 9 | `09-lab-reservation` | 校园实验室预约管理系统 | 8109 | 5109 | tech |

## 统一工程基线
Java 21、Spring Boot 3.5.16、Spring Security、Spring Data JPA、Bean Validation、H2（默认）/MySQL（profile）、Vue 3 + Vite。默认账号见各项目 README。

## 目录规范
每个项目固定包含 `backend/`、`frontend/`、`README.md`，没有跨目录 Maven/npm 依赖。第 8 项标题只要求 Spring Boot，但仍额外提供了独立 Vue 前端，便于直接演示完整 B/S 系统。

## 一键检查
有 Maven/npm 环境时运行根目录 `./verify.sh`。脚本会依次执行 9 个后端 `mvn test` 与 9 个前端 `npm install && npm run build`；任何一步失败即退出非零状态。

## 数据库
生产默认使用 `prod` profile 并强制外部 MySQL 凭据；本地演示必须显式启用 `dev` profile 才会使用 H2 与演示账号。JPA 自动维护演示表结构，避免首次启动必须手动导入 SQL。

## Production hardening baseline

生产 profile 使用 MySQL + Flyway，Hibernate 仅 `validate`，且 `DB_URL` / `DB_USER` / `DB_PASSWORD` / `APP_CORS_ALLOWED_ORIGINS` 必须由部署环境提供；dev/test/ci 才会创建演示账号。API 使用无状态 opaque Bearer Token（256-bit CSPRNG，数据库仅存 SHA-256 摘要）、BCrypt cost 12、认证端点限速、严格 CORS、安全响应头、统一错误边界、乐观锁、事务与数据库约束。Actuator 仅暴露 health/info/prometheus。

仍应在真实部署层配置 TLS、WAF/反向代理限速、秘密管理、数据库备份、集中日志/告警和最小网络权限；应用内限速是单节点防御层，不替代边缘限速。

## Validation provenance

仓库中的 `.github/workflows/production-suite.yml` 直接针对本目录中的真实源码执行验证，不依赖压缩包、patch 或 reconstruction：9 个后端执行完整 Maven verify、MySQL 8.4 + Flyway/Hibernate 生产启动和非 root 容器构建；9 个前端使用已提交 lockfile 执行可复现 npm ci、生产依赖审计与构建；另执行生产策略不变量以及 Java/JavaScript CodeQL。
