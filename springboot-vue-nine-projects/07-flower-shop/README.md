# 7. 花卉商品在线销售管理系统

独立全栈项目。后端：Java 21 + Spring Boot 3.5.16 + Spring Security + Spring Data JPA；前端：Vue 3 + Vite。

## 已实现
花卉商品、库存、配送信息、事务型下单与库存扣减。公共基础设施包含持久化 Bearer Token 登录、USER/ADMIN RBAC、参数校验、统一异常、H2/MySQL 双数据库配置、初始化演示数据。

## 启动
1. `cd backend && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run`（默认 H2，端口 8107）
2. `cd frontend && npm install && npm run dev`（端口 5107）
3. 浏览器打开 `http://localhost:5107`。

MySQL：`mvn spring-boot:run -Dspring-boot.run.profiles=mysql`，并可用 `DB_URL`、`DB_USER`、`DB_PASSWORD` 覆盖连接参数。

演示账号：`admin / Admin123!Demo`；`student / Student123!Demo`。

## 验证
- 后端：`mvn test`
- 前端：`npm run build`

## 生产启动
默认 profile 为 `prod`（fail-closed）。必须提供 `DB_URL`、`DB_USER`、`DB_PASSWORD`、`APP_CORS_ALLOWED_ORIGINS`；不要在生产环境启用 `dev/test/ci` profile。
