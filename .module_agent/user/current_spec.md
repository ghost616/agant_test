用户模块，包含用户管理、用户权限、用户登录、用户会话功能。
- 用户实体：用户ID、登录名(login_name)、显示名(display_name)、用户类型(user_type: 1普通/2管理员)、密码(password)、enabled 登录开关
- 默认管理员：id=1、login_name=admin、display_name=管理员、密码 123456
- 登录接口 POST /api/auth/login；用户管理接口（列表/添加/修改/禁止登录，仅管理员）
- 用户会话管理：内存存储，空闲 2 小时过期，会话Id 写入 HttpOnly Cookie
## 用户会话管理

- UserSession：用户会话对象，含会话 ID、登录用户信息（User 实体）、最后访问时间（epoch 毫秒，volatile）。
- UserContext：ThreadLocal 线程上下文，静态 set/get/clear 保存当前请求线程的用户会话，供后续鉴权拦截器写入、业务代码读取。
- UserSessionManager：内存会话管理器（ConcurrentHashMap），按会话 ID 创建/查询/删除/刷新会话；会话 Cookie 名称 SESSION_ID，空闲 2 小时过期；@Scheduled 每分钟清理过期会话。
- UserContextUtil：静态工具类，其它模块获取当前登录用户 ID 的统一入口。requireUserId() 会话或用户为空时抛 BusinessException(ErrorCode.USER_NOT_LOGIN)；currentUserIdOrNull() 会话或用户为空时返回 null（不抛异常）。内部通过 UserContext 读取当前线程用户会话。
## 登录与认证

- AuthController：POST /api/auth/login（豁免鉴权），校验 LoginRequest（登录名+密码），成功则创建用户会话并写入 HttpOnly 会话 Cookie（SameSite=Lax），返回 UserDTO。
- POST /api/auth/logout：清除服务端会话（userSessionManager.removeSession）并通过 Set-Cookie maxAge=0 删除 HttpOnly 会话 Cookie，返回成功；仅要求登录，普通用户可调用。
- PUT /api/auth/me：当前登录用户自助修改自己的显示名与密码（字段为空不修改，enabled 不可自助修改），通过 UserContextUtil.requireUserId() 从 UserContext 获取当前用户，返回修改后的 UserDTO；仅要求登录。
- 登录失败（用户不存在或密码错误）抛 USER_LOGIN_FAILED；账号被禁止登录（enabled=0）抛 USER_DISABLED。
- 密码校验复用 PasswordUtil.encrypt(明文, 用户ID, 创建时间毫秒)，创建时间使用秒级精度 LocalDateTime 以保障写入/读取往返后加密结果一致。
## 用户管理与服务

- UserController：用户管理接口（仅管理员 user_type=2 可调用），通过请求 Cookie 解析会话校验权限，未登录抛 USER_NOT_LOGIN，非管理员抛 USER_FORBIDDEN。
  - GET /api/users 分页列表（page/size，按创建时间倒序，仅返回普通用户 userType=1，分页总数同步过滤）
  - POST /api/users 添加用户（校验登录名唯一，固定为普通用户 userType=1，默认允许登录 enabled=1）
  - PUT /api/users/{id} 修改用户（显示名/密码/登录开关，空字段不修改；不再支持修改用户类型）
- UserService/UserServiceImpl：登录校验、分页查询、添加/修改用户、自助修改；添加用户预生成雪花 ID（IdWorker）后按加密公式加密密码；enabled 仅允许 0/1。
- updateSelf(userId, UserSelfUpdateRequest)：当前登录用户自助修改自己的显示名/密码（字段为空不修改），enabled 不可自助修改。
- DTO：LoginRequest、UserDTO（不含密码，id 字符串序列化）、UserCreateRequest（loginName/password 必填，displayName/enabled 可选）、UserUpdateRequest（displayName/password/enabled 可选）、UserSelfUpdateRequest（displayName/password 可选）。
## 初始化与错误码

- DefaultAdminInitializer（ApplicationRunner）：启动时若 user 表为空，插入 id=1、login_name=admin、display_name=管理员、user_type=2 的默认管理员，密码为加密后的 123456。
- ErrorCode 扩展：新增 USER_LOGIN_FAILED（登录名或密码错误）、USER_DISABLED（账号已被禁止登录）；同步更新 ErrorCodeTest 枚举数量与错误码映射断言。
## 前端类型定义

- src/types/user.ts：User（id 为字符串）、LoginRequest、UserCreateRequest、UserUpdateRequest、UserSelfUpdateRequest（displayName/password 可选）类型定义，及用户类型常量 USER_TYPE_NORMAL=1、USER_TYPE_ADMIN=2；UserCreateRequest/UserUpdateRequest 不再包含 userType 字段。
- src/pages/users/UserList.tsx：用户管理页移除用户类型列与表单字段（新建用户固定为普通用户），仅保留显示名/密码/登录开关管理；列表接口仅返回普通用户。