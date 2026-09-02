# 设计参考与取舍

本套代码没有直接复制第三方项目源码，只参考其工程边界和功能拆分。

- YunaiV/ruoyi-vue-pro：参考 Spring Security、Token、RBAC、前后端分离、多数据库和模块化边界；本项目刻意去掉 Redis、工作流、多租户等毕业设计不必要的重依赖。
- yudaocode/yudao-boot-mini：参考“精简公共基础设施 + 按业务迁移模块”的思路。
- vbenjs/vue-vben-admin：参考 Vue 3 管理端的组件化、清晰信息层级；本套 9 个前端未复制其组件与样式。
- 1120041844/CampusClubManagementSystem：参考高校社团领域题材拆分。
- zongjixiaoai66/LaboratoryManagementSystem：参考实验室、实验课程、预约、设备等领域边界；本项目聚焦实验室资源与预约冲突这一核心流程。

技术版本固定：Java 21 + Spring Boot 3.5.16；前端 Vue 3 + Vite。
