# 验证报告

生成日期：2026-09-02。

## 已验证

- 目录完整性：9 个项目均含独立 `backend/`、`frontend/`、README；无跨项目 Maven/npm 运行依赖。
- 端口与主题：9 个后端端口、9 个前端端口互不重复；9 份 CSS 主题文件互不相同。
- Java 语法：通过 JDK `JavacTask.parse()` 对全部 230 个 `.java` 文件做真实编译器前端语法解析，0 syntax error。
- 前端 JavaScript 语法：通过 Node `--check` 检查 9 份 Vue `<script setup>`、9 份 `api.js`、9 份 `main.js`、9 份 `vite.config.js`，共 36 个模块，全部通过。
- 静态语义检查：POM XML 与 package.json 可解析；Java package/path 一致；项目内部 import 可解析；无生成占位符；初始化顺序正确；管理员用户 DTO 不含密码哈希；关键唯一约束存在；实验室预约使用实验室行悲观锁串行化冲突检查。
- 安全复核：密码使用 BCrypt；Bearer Token 使用 256-bit `SecureRandom`，数据库只保存 SHA-256 哈希；用户实体密码哈希标记 `@JsonIgnore`；REST API 为无状态认证；普通用户与管理员通过 Spring Method Security 分权。
- 业务并发：所有实体含 `@Version` 乐观锁；团购、鲜花库存和图书副本在事务内更新；实验室预约额外使用 `PESSIMISTIC_WRITE` 锁住实验室资源后再做区间冲突判断。
- 测试源码：每个项目含上下文测试、认证流程测试、核心业务流程/边界回归测试。

## 当前环境未能验证

当前执行环境没有 Maven，且网络 DNS/依赖下载被禁用；`npm install` 也因无法联网超时。因此本次会话中**没有实际完成依赖解析后的 `mvn test` 和 `npm run build`**。这意味着不能诚实地宣称“100% 构建验证通过”。根目录 `verify.sh` 已提供完整的 18 阶段验证入口；在可联网并装有 Maven/Node 的环境运行后，只有全部通过才应视为最终构建闭环。

离线可重复的语法/结构验证可运行：`./verify-syntax.sh`。
