# DDD Admin 企业级后台管理系统 — 构建实录

> 日期：2026-06-04  
> 技术栈：Spring Boot 3.3.5 + Shiro 1.13.0 + MyBatis-Plus 3.5.7 + Vue 3.5 + Vite 6 + Element Plus

---

## 一、项目概述

从零构建了一个完整的企业级后台管理系统，包含：

- **后端**：Spring Boot 3.3.5 + MySQL 8.0 + Redis + Shiro + JWT + MyBatis-Plus
- **前端**：Vue 3.5 + Vite 6 + Element Plus + Pinia + Axios
- **功能**：JWT 无状态登录、图形验证码、RBAC 角色鉴权、用户 CRUD、系统硬件仪表盘

---

## 二、后端实现

### 2.1 数据库设计

**数据库：`ddd`**（utf8mb4，支持中文）

```sql
-- 三张核心表 + 预置数据
sys_user       -- 用户表（17字段：用户名、BCrypt密码、手机、邮箱、状态等）
sys_role       -- 角色表（SUPER_ADMIN / ADMIN / USER / AUDITOR）
sys_user_role  -- 用户角色关系表（多对多）
```

预置用户：`admin` / `zhangsan` / `lisi` / `wangwu`（密码均为 `admin123`）

### 2.2 项目结构（37个文件）

```
ddd-admin/
├── pom.xml
└── src/main/java/com/ddd/admin/
    ├── DddAdminApplication.java          # 启动类
    ├── common/
    │   ├── Result.java                   # 统一响应 { code, message, data }
    │   ├── ResultCode.java               # 状态码枚举（15+）
    │   ├── BusinessException.java        # 业务异常
    │   └── GlobalExceptionHandler.java   # 全局异常处理（12种异常）
    ├── entity/
    │   ├── SysUser.java                  # 用户实体（@TableLogic逻辑删除）
    │   ├── SysRole.java
    │   └── SysUserRole.java
    ├── mapper/
    │   ├── SysUserMapper.java            # 含角色查询SQL
    │   ├── SysRoleMapper.java
    │   └── SysUserRoleMapper.java
    ├── service/
    │   ├── SysUserService.java           # 接口（11个方法）
    │   ├── SysRoleService.java
    │   ├── SysUserRoleService.java
    │   └── impl/                         # 实现类
    ├── dto/
    │   ├── LoginDTO.java                 # 登录请求（@Valid校验）
    │   ├── TokenDTO.java                 # Token响应
    │   ├── UserInfoDTO.java              # 用户信息
    │   ├── UserQueryDTO.java             # 用户查询+分页
    │   └── UserCreateDTO.java            # 用户创建/更新
    ├── security/
    │   ├── JwtToken.java                 # Shiro自定义Token
    │   ├── JwtUtils.java                 # JWT生成/解析/验证/黑名单
    │   ├── JwtFilter.java                # 认证过滤器（OncePerRequestFilter）
    │   └── UserRealm.java                # Shiro认证授权Realm
    ├── config/
    │   ├── ShiroConfig.java              # Shiro无状态配置
    │   ├── MyBatisPlusConfig.java        # 分页/自动填充/乐观锁
    │   ├── RedisConfig.java              # Redis序列化
    │   ├── CorsConfig.java               # 跨域
    │   ├── KaptchaConfig.java            # 验证码
    │   └── DataInitializer.java          # 密码自动加密
    └── controller/
        ├── AuthController.java           # 登录/退出/验证码/Token刷新
        ├── UserController.java           # 用户CRUD（@RequiresRoles鉴权）
        └── SystemController.java         # 硬件信息（纯JDK实现）
```

### 2.3 核心功能实现

#### 认证流程

```
POST /api/auth/login
  → AuthController: 验证码校验（Redis）
  → SysUserService.login():
      1. Redis检查锁定状态
      2. DB查询用户
      3. BCrypt.checkpw() 密码校验
      4. 登录失败计数（5次锁定15分钟）
      5. 生成JWT（2h AccessToken + 7d RefreshToken）
      6. 记录最后登录IP和时间
```

#### JWT 过滤器架构

```
请求 → JwtFilter(OncePerRequestFilter)     ← Spring原生，jakarta.servlet
         ├─ 公开路径？→ 放行
         ├─ 提取 Authorization: Bearer <token>
         ├─ JwtUtils.validateToken()
         └─ new Subject.Builder(securityManager).buildSubject().login()
              └─ UserRealm.doGetAuthenticationInfo()
                   ↓
       ShiroFilter（全部anon，维护Subject生命周期）
              ↓
       Controller → @RequiresRoles（Shiro AOP鉴权）
         └─ 请求结束 → ThreadContext.unbindSubject()
```

#### 企业级特性

| 功能 | 实现 |
|------|------|
| BCrypt加密 | Hutool `BCrypt.hashpw()` / `BCrypt.checkpw()` |
| 图形验证码 | Kaptcha生成 → Base64返回 → Redis 5分钟有效 |
| 登录锁定 | Redis计数器：5次失败 → 15分钟锁定 |
| Token黑名单 | 退出登录时Token加入Redis黑名单（TTL=剩余有效期） |
| 逻辑删除 | MyBatis-Plus `@TableLogic` |
| 自动填充 | createdTime/updatedTime/createdBy/updatedBy |
| 分页 | PaginationInnerInterceptor（MySQL方言，上限500） |
| 防全表删改 | BlockAttackInnerInterceptor |
| 角色鉴权 | `@RequiresRoles({"SUPER_ADMIN", "ADMIN"})` |

---

## 三、前端实现

### 3.1 项目结构（18个文件）

```
ddd-admin-web/
├── package.json              # Vue3+Vite6+ElementPlus+Axios+Pinia
├── vite.config.js            # 代理/api → localhost:8080，自动导入组件
├── index.html
├── .env.development
└── src/
    ├── main.js               # 注册Pinia/Router/ElementPlus（中文+全图标）
    ├── App.vue               # 根组件
    ├── router/index.js       # 路由配置+守卫（Token校验/重定向）
    ├── stores/auth.js        # Pinia认证Store（12个action）
    ├── api/
    │   ├── request.js        # Axios封装（Token自动附带/401刷新队列）
    │   ├── auth.js           # 认证API
    │   ├── user.js           # 用户CRUD API（8个接口）
    │   └── system.js         # 硬件信息API
    ├── utils/token.js        # Token存取（localStorage）
    ├── layout/Layout.vue     # 后台布局（可折叠侧边栏+顶栏+面包屑）
    └── views/
        ├── login/LoginPage.vue           # 登录页（表单校验+验证码）
        ├── dashboard/DashboardPage.vue   # 硬件仪表盘
        ├── user/
        │   ├── UserListPage.vue          # 用户列表（搜索+分页+操作）
        │   └── UserFormDialog.vue        # 新增/编辑弹窗（角色多选）
        └── error/403.vue, 404.vue
```

### 3.2 核心实现

#### Axios 拦截器

```
请求拦截器 → 自动附加 Authorization: Bearer <token>

响应拦截器：
  code=200 → 返回 data
  code=401 → 尝试 RefreshToken → 成功重放 / 失败跳/login
  并发401 → 队列等待（避免重复刷新）
  code=403 → 提示"无权限"
  code=500 → 提示"服务器异常"
```

#### 路由守卫

```
router.beforeEach:
  访问/login + 有Token → 跳转/dashboard
  访问/login + 无Token → 放行
  访问其他   + 无Token → 跳转/login?redirect=xxx
  访问其他   + 有Token → 放行
```

#### 用户管理数据流

```
UserListPage.mounted()
  → nextTick() → fetchData()
    → GET /api/user/list?page=1&pageSize=10
    → 渲染表格 + 分页

新增用户：
  点击"新增" → UserFormDialog打开
  → 填写表单（含角色多选）→ 校验
  → POST /api/user → 创建成功 → 刷新列表

编辑/删除/重置密码 → 对应PUT/DELETE请求
```

#### 硬件仪表盘

```
DashboardPage.mounted()
  → fetchHardwareInfo()
  → GET /api/system/info
  → 渲染CPU/GPU/内存/磁盘卡片
  → setInterval(3000ms) 自动刷新
```

---

## 四、踩坑记录与解决方案

### 坑1：Shiro javax.servlet vs Spring Boot 3 jakarta.servlet ⭐ 最坑

**现象**：`JwtFilter extends AuthenticatingFilter` 报错 `createToken(javax.servlet.ServletRequest)` 抽象方法未覆盖。

**根因**：Shiro 1.13.0 的 `AuthenticatingFilter` 方法签名用 `javax.servlet.ServletRequest`，但 Spring Boot 3 内置 Tomcat 10 只有 `jakarta.servlet.ServletRequest`。

**尝试过的方案**：
- ❌ 升级 Shiro 到 2.0.0 → Maven 拉不到
- ❌ 降级用 javax → Tomcat 10 完全不支持

**最终方案**：`JwtFilter` 不继承 `AuthenticatingFilter`，改为继承 Spring 原生的 `OncePerRequestFilter`（jakarta.servlet），在 `doFilterInternal()` 中手动调用 `Subject.login()`。

### 坑2：Nginx SecurityManager 未绑定

**现象**：`JWT认证失败: No SecurityManager accessible to the calling code`

**根因**：`SecurityUtils.getSubject()` 依赖 ThreadContext 中的 SecurityManager，但 Spring Filter 先于 ShiroFilter 执行，此时尚未绑定。

**最终方案**：
1. `ShiroConfig` 中显式 `SecurityUtils.setSecurityManager(securityManager)` 注册静态单例
2. `JwtFilter` 注入 `SecurityManager`，用 `new Subject.Builder(securityManager).buildSubject()` 代替 `SecurityUtils.getSubject()`
3. 请求结束 `ThreadContext.unbindSubject()` 清理

### 坑3：Shiro Realm 注入到错误的 Authenticator

**现象**：`No realms have been configured! One or more realms must be present`

**根因**：`setRealm()` 触发 `afterRealmsSet()` 把 Realm 注入到 `当前` Authenticator。先调 `setRealm()` 再 `setAuthenticator(new one)` 导致 Realm 注入到默认认证器，新认证器为空。

**最终方案**：调换顺序 —— 先 `setAuthenticator()` 再 `setRealm()`。

### 坑4：数据库密码是占位 BCrypt Hash

**现象**：登录时 `admin/admin123` 提示密码错误。

**根因**：SQL 建表时插入的 `$2a$10$N.zmdr9k...` 是随手写的占位符，不是真正对 `admin123` 的 BCrypt 加密结果。`DataInitializer` 检查 `startsWith("$2a$")` 误判为"已加密"跳过了修复。

**最终方案**：
1. 用 `bcryptjs` 生成正确 hash，直接 UPDATE 数据库
2. 修复 `DataInitializer` 检测条件为 `startsWith("$2")`（兼容 `$2a$` 和 `$2b$`）

### 坑5：用户列表死循环请求

**现象**：前端一直显示加载图标，后端 SQL 日志疯狂打印。

**根因**：`el-pagination` 的 `v-model:current-page` + `@current-change` 在组件初始化时产生事件回环 → `fetchData()` 被无限重复调用。

**最终方案**：
1. `v-model` 改为单向绑定 `:current-page` + `@update:current-page`
2. 事件处理函数加判重守卫：`if (queryPage === page) return`
3. 加 `fetching` 防重入锁：`if (fetching) return`
4. 加 `fetchSeq` 序列号：过期响应结果丢弃
5. `onMounted` 中用 `nextTick()` 延迟初始加载

### 坑6：退出登录导致级联重复请求

**现象**：退出登录后后台打印多次 JWT 认证失败日志。

**根因**：`/api/auth/logout` 不在 `JwtFilter.PUBLIC_PATHS` 中 → 退出请求被 JWT 认证拦截 → 认证失败返回 401 → Axios 拦截器触发 Token 刷新 → 级联重复。

**最终方案**：将 `/api/auth/logout` 加入公开路径，Controller 自行从 Header 提取 Token 处理黑名单。

### 坑7：MySQL JDBC 字符编码

**现象**：`Unsupported character encoding 'utf8mb4'`

**根因**：`utf8mb4` 是 MySQL 层面的编码名，JDBC 驱动只认 Java 编码名。

**最终方案**：URL 改为 `characterEncoding=UTF-8`，驱动自动映射到 MySQL 的 `utf8mb4`。

### 坑8：Maven/IDEA 编译编码

**现象**：命令行 `mvn compile` 报非法字符错误。

**根因**：Windows 中文系统 Maven 默认用 GBK 编码编译 UTF-8 源文件，特殊字符和中文注释无法识别。

**最终方案**：`pom.xml` 中 `<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>` + maven-compiler-plugin 显式 `<encoding>UTF-8</encoding>`。IDEA 内部编译不受影响。

### 坑9：OSHI 硬件库版本兼容

**现象**：`FileStore` 类找不到、`getVram()` 方法不存在。

**根因**：OSHI 6.4.x 将 `FileStore` 重命名为 `OSFileStore`，不同版本 API 变动频繁。

**最终方案**：**彻底移除 OSHI 依赖**，改用纯 JDK API：
- CPU：`PROCESSOR_IDENTIFIER` 环境变量 + `OperatingSystemMXBean.getCpuLoad()`
- GPU：`wmic path win32_VideoController` + `nvidia-smi`
- 内存：`OperatingSystemMXBean.getTotalMemorySize()`
- 磁盘：`File.listRoots()` + `getTotalSpace()`

---

## 五、系统架构总览

```
┌─────────────────────────────────────────────────────┐
│                    Browser (Vue3)                    │
│  localhost:5173                                     │
│  Login → Dashboard → User CRUD → System Monitor     │
├──────────────────────┬──────────────────────────────┤
│   Axios + JWT        │  Vite Proxy /api → :8080     │
├──────────────────────▼──────────────────────────────┤
│              Spring Boot 3.3.5 :8080                │
│  ┌─────────────────────────────────────────────┐    │
│  │  JwtFilter (OncePerRequestFilter)           │    │
│  │    → 提取JWT → Subject.login()              │    │
│  ├─────────────────────────────────────────────┤    │
│  │  ShiroFilter (Subject生命周期)               │    │
│  │    → @RequiresRoles AOP鉴权                 │    │
│  ├─────────────────────────────────────────────┤    │
│  │  Controllers (Auth/User/System)             │    │
│  ├─────────────────────────────────────────────┤    │
│  │  MyBatis-Plus → MySQL 8.0 (ddd)             │    │
│  │  Redis (Token/验证码/限流)                   │    │
│  └─────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────┘
```

---

## 六、启动方式

### 后端

```bash
# IDEA中打开 ddd-admin → 右键 pom.xml → Maven Reload
# 运行 DddAdminApplication.java
# 确保 MySQL 和 Redis 已启动
```

### 前端

```bash
cd ddd-admin-web
npm install
npm run dev
# 访问 http://localhost:5173
# 登录: admin / admin123
```

---

## 七、可优化方向

| 优先级 | 内容 |
|--------|------|
| 🔴 高 | JWT密钥改用环境变量；日志脱敏；加独立权限表 |
| 🟡 中 | RefreshToken Rotation；操作审计日志；接口限流 |
| 🟢 低 | Swagger文档；Docker部署；单元测试 |
