package com.ghost616.platform.service.user;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ghost616.platform.dto.PageResult;
import com.ghost616.platform.dto.user.UserCreateRequest;
import com.ghost616.platform.dto.user.UserDTO;
import com.ghost616.platform.dto.user.UserSelfUpdateRequest;
import com.ghost616.platform.dto.user.UserUpdateRequest;
import com.ghost616.platform.entity.User;
import com.ghost616.platform.enums.ErrorCode;
import com.ghost616.platform.exception.BusinessException;
import com.ghost616.platform.repository.UserMapper;
import com.ghost616.platform.util.PasswordUtil;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.ResultHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UserServiceImpl 单元测试（不使用 Mockito，采用手写 Mapper 桩，与项目测试约定一致）。
 */
class UserServiceImplTest {

    /**
     * 初始化 User 实体的 MyBatis-Plus TableInfo 缓存，使 LambdaQueryWrapper
     * 在无 Spring 上下文时也能解析 lambda 对应的列名。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
    }

    /** 可编程 UserMapper 桩：记录 Wrapper 与写入实体，并模拟 DB 过滤行为。 */
    static class StubUserMapper implements UserMapper {

        final List<User> table = new ArrayList<>();

        Wrapper<User> lastSelectPageWrapper;
        Wrapper<User> lastSelectOneWrapper;
        Wrapper<User> lastSelectCountWrapper;
        User lastInserted;
        User lastUpdated;

        @Override
        public int insert(User entity) {
            table.add(entity);
            lastInserted = entity;
            return 1;
        }

        @Override
        public int updateById(User entity) {
            for (int i = 0; i < table.size(); i++) {
                if (table.get(i).getId().equals(entity.getId())) {
                    table.set(i, entity);
                    lastUpdated = entity;
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public User selectById(Serializable id) {
            for (User u : table) {
                if (u.getId().equals(id)) {
                    return u;
                }
            }
            return null;
        }

        @Override
        public Long selectCount(Wrapper<User> queryWrapper) {
            lastSelectCountWrapper = queryWrapper;
            return (long) filterByWrapper(table, queryWrapper).size();
        }

        @Override
        public User selectOne(Wrapper<User> queryWrapper) {
            lastSelectOneWrapper = queryWrapper;
            List<User> matched = filterByWrapper(table, queryWrapper);
            return matched.isEmpty() ? null : matched.get(0);
        }

        @Override
        public <P extends IPage<User>> P selectPage(P page, Wrapper<User> queryWrapper) {
            lastSelectPageWrapper = queryWrapper;
            List<User> matched = filterByWrapper(table, queryWrapper);
            Page<User> p = (Page<User>) page;
            p.setRecords(new ArrayList<>(matched));
            p.setTotal(matched.size());
            return page;
        }

        private List<User> filterByWrapper(List<User> source, Wrapper<User> wrapper) {
            List<User> result = new ArrayList<>();
            for (User u : source) {
                if (matches(u, wrapper)) {
                    result.add(u);
                }
            }
            return result;
        }

        private boolean matches(User user, Wrapper<User> wrapper) {
            if (wrapper == null) {
                return true;
            }
            if (wrapper instanceof AbstractWrapper<?, ?, ?>) {
                AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
                String segment = abstractWrapper.getSqlSegment();
                Collection<Object> values = abstractWrapper.getParamNameValuePairs().values();
                if (segment != null && segment.contains("user_type") && values.contains(1)) {
                    return user.getUserType() != null && user.getUserType() == 1;
                }
            }
            return true;
        }

        @Override
        public int deleteById(User entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int delete(Wrapper<User> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(User entity, Wrapper<User> updateWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> selectBatchIds(Collection<? extends Serializable> idList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectBatchIds(Collection<? extends Serializable> idList, ResultHandler<User> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> selectList(Wrapper<User> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(Wrapper<User> queryWrapper, ResultHandler<User> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<User> selectList(IPage<User> page, Wrapper<User> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectList(IPage<User> page, Wrapper<User> queryWrapper, ResultHandler<User> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(Wrapper<User> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(Wrapper<User> queryWrapper, ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Map<String, Object>> selectMaps(IPage<? extends Map<String, Object>> page, Wrapper<User> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void selectMaps(IPage<? extends Map<String, Object>> page, Wrapper<User> queryWrapper,
                               ResultHandler<Map<String, Object>> resultHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> List<E> selectObjs(Wrapper<User> queryWrapper) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <E> void selectObjs(Wrapper<User> queryWrapper, ResultHandler<E> resultHandler) {
            throw new UnsupportedOperationException();
        }
    }

    private User newUser(Long id, String loginName, int userType, int enabled, LocalDateTime createTime) {
        User user = new User();
        user.setId(id);
        user.setLoginName(loginName);
        user.setDisplayName(loginName);
        user.setUserType(userType);
        user.setEnabled(enabled);
        user.setCreateTime(createTime);
        return user;
    }

    private StubUserMapper mapperWith(StubUserMapper mapper, User... users) {
        for (User u : users) {
            mapper.table.add(u);
        }
        return mapper;
    }

    // ================= 功能 1：分页查询仅返回普通用户 =================

    @Test
    void pageUsers_查询条件包含userType1过滤() {
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "normal-a", UserService.USER_TYPE_NORMAL, 1, LocalDateTime.now()),
                newUser(2L, "admin-a", UserService.USER_TYPE_ADMIN, 1, LocalDateTime.now()),
                newUser(3L, "normal-b", UserService.USER_TYPE_NORMAL, 1, LocalDateTime.now()));
        UserServiceImpl service = new UserServiceImpl(mapper);

        PageResult<UserDTO> result = service.pageUsers(1, 10);

        Wrapper<User> wrapper = mapper.lastSelectPageWrapper;
        assertNotNull(wrapper, "分页查询必须携带查询条件");
        assertTrue(wrapper instanceof AbstractWrapper<?, ?, ?>, "查询条件应为可提取参数的条件包装器");
        AbstractWrapper<?, ?, ?> abstractWrapper = (AbstractWrapper<?, ?, ?>) wrapper;
        String segment = abstractWrapper.getSqlSegment();
        assertTrue(segment != null && segment.contains("user_type"),
                "查询条件必须包含 user_type 过滤，实际 SQL: " + segment);
        assertTrue(abstractWrapper.getParamNameValuePairs().containsValue(1),
                "user_type 过滤值必须为普通用户类型 1");
    }

    @Test
    void pageUsers_结果集仅含普通用户且total不含管理员() {
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "normal-a", UserService.USER_TYPE_NORMAL, 1, LocalDateTime.now()),
                newUser(2L, "admin-a", UserService.USER_TYPE_ADMIN, 1, LocalDateTime.now()),
                newUser(3L, "normal-b", UserService.USER_TYPE_NORMAL, 1, LocalDateTime.now()),
                newUser(4L, "admin-b", UserService.USER_TYPE_ADMIN, 1, LocalDateTime.now()));
        UserServiceImpl service = new UserServiceImpl(mapper);

        PageResult<UserDTO> result = service.pageUsers(1, 10);

        assertEquals(2, result.getTotal(), "total 不应包含管理员数量");
        assertEquals(2, result.getList().size());
        for (UserDTO dto : result.getList()) {
            assertEquals(UserService.USER_TYPE_NORMAL, dto.getUserType(),
                    "结果集中不应出现管理员");
        }
        assertFalse(result.getList().stream().anyMatch(dto -> dto.getLoginName().startsWith("admin")),
                "管理员不应进入结果集");
    }

    @Test
    void pageUsers_边界页码与分页参数透传() {
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "n1", UserService.USER_TYPE_NORMAL, 1, LocalDateTime.now()));
        UserServiceImpl service = new UserServiceImpl(mapper);

        PageResult<UserDTO> result = service.pageUsers(0, 0);
        assertEquals(1, result.getPage(), "page 小于 1 时应归一为 1");
        assertEquals(10, result.getSize(), "size 不大于 0 时应使用默认 10");

        PageResult<UserDTO> result2 = service.pageUsers(-5, -3);
        assertEquals(1, result2.getPage());
        assertEquals(10, result2.getSize());
    }

    // ================= 功能 2：添加用户固定为普通用户 =================

    @Test
    void createUser_固定普通用户类型且enabled缺省为1() {
        StubUserMapper mapper = new StubUserMapper();
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserCreateRequest request = UserCreateRequest.builder()
                .loginName("zhangsan")
                .displayName("张三")
                .password("pass123")
                .build();
        UserDTO dto = service.createUser(request);

        assertNotNull(mapper.lastInserted);
        assertEquals(UserService.USER_TYPE_NORMAL, mapper.lastInserted.getUserType(),
                "新建用户必须固定为普通用户类型 1");
        assertEquals(Integer.valueOf(1), mapper.lastInserted.getEnabled(),
                "enabled 缺省应为 1（允许登录）");
        assertNotNull(mapper.lastInserted.getId());
        assertNotNull(mapper.lastInserted.getCreateTime());
        assertEquals(UserService.USER_TYPE_NORMAL, dto.getUserType());
    }

    @Test
    void createUser_enabled显式传0时保留() {
        StubUserMapper mapper = new StubUserMapper();
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserCreateRequest request = UserCreateRequest.builder()
                .loginName("lisi")
                .password("pass456")
                .enabled(0)
                .build();
        service.createUser(request);

        assertEquals(Integer.valueOf(0), mapper.lastInserted.getEnabled());
    }

    @Test
    void createUser_密码按加密公式加密() {
        StubUserMapper mapper = new StubUserMapper();
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserCreateRequest request = UserCreateRequest.builder()
                .loginName("wangwu")
                .password("MyP@ss-2026")
                .build();
        service.createUser(request);

        LocalDateTime createTime = mapper.lastInserted.getCreateTime();
        long createTimeMillis = createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String expected = PasswordUtil.encrypt("MyP@ss-2026",
                String.valueOf(mapper.lastInserted.getId()), createTimeMillis);
        assertEquals(expected, mapper.lastInserted.getPassword(),
                "密码必须按 SM3(MD5(明文+用户ID)+创建时间毫秒) 公式加密");
    }

    @Test
    void createUser_登录名重复抛USER_ALREADY_EXISTS() {
        LocalDateTime now = LocalDateTime.now();
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "dup", UserService.USER_TYPE_NORMAL, 1, now));
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserCreateRequest request = UserCreateRequest.builder()
                .loginName("dup")
                .password("x123456")
                .build();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.createUser(request));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
    }

    // ================= 功能 3：修改用户不再支持用户类型 =================

    @Test
    void updateUser_修改显示名密码enabled且不动用户类型() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "n1", UserService.USER_TYPE_NORMAL, 1, now));
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("新显示名")
                .password("newPass")
                .enabled(0)
                .build();
        UserDTO dto = service.updateUser(1L, request);

        assertEquals("新显示名", mapper.lastUpdated.getDisplayName());
        assertEquals(Integer.valueOf(0), mapper.lastUpdated.getEnabled());
        assertEquals(UserService.USER_TYPE_NORMAL, mapper.lastUpdated.getUserType(),
                "修改用户不得改变用户类型");
        LocalDateTime createTime = mapper.lastUpdated.getCreateTime();
        long createTimeMillis = createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(PasswordUtil.encrypt("newPass", "1", createTimeMillis),
                mapper.lastUpdated.getPassword());
        assertEquals("新显示名", dto.getDisplayName());
    }

    @Test
    void updateUser_字段为空表示不修改() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        User original = newUser(1L, "n1", UserService.USER_TYPE_NORMAL, 1, now);
        original.setPassword(PasswordUtil.encrypt("old", "1",
                now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        StubUserMapper mapper = mapperWith(new StubUserMapper(), original);
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserUpdateRequest request = UserUpdateRequest.builder().build();
        service.updateUser(1L, request);

        assertEquals("n1", mapper.lastUpdated.getDisplayName(), "空字段不应修改显示名");
        assertEquals(original.getPassword(), mapper.lastUpdated.getPassword(), "空字段不应修改密码");
        assertEquals(Integer.valueOf(1), mapper.lastUpdated.getEnabled(), "空字段不应修改 enabled");
    }

    @Test
    void updateUser_enabled非法值抛PARAM_INVALID() {
        LocalDateTime now = LocalDateTime.now();
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "n1", UserService.USER_TYPE_NORMAL, 1, now));
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserUpdateRequest request = UserUpdateRequest.builder().enabled(2).build();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateUser(1L, request));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void updateUser_用户不存在抛USER_NOT_FOUND() {
        StubUserMapper mapper = new StubUserMapper();
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserUpdateRequest request = UserUpdateRequest.builder().displayName("x").build();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateUser(99L, request));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void updateUser_无创建时间重置密码抛PARAM_INVALID() {
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "n1", UserService.USER_TYPE_NORMAL, 1, null));
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserUpdateRequest request = UserUpdateRequest.builder().password("newPass").build();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateUser(1L, request));
        assertEquals(ErrorCode.PARAM_INVALID, ex.getErrorCode());
    }

    // ================= 功能 4：自助修改 =================

    @Test
    void updateSelf_修改自己的显示名与密码() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "self", UserService.USER_TYPE_NORMAL, 1, now));
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder()
                .displayName("新昵称")
                .password("newPwd")
                .build();
        UserDTO dto = service.updateSelf(1L, request);

        assertEquals("新昵称", mapper.lastUpdated.getDisplayName());
        LocalDateTime createTime = mapper.lastUpdated.getCreateTime();
        long createTimeMillis = createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(PasswordUtil.encrypt("newPwd", "1", createTimeMillis),
                mapper.lastUpdated.getPassword());
        assertEquals("新昵称", dto.getDisplayName());
    }

    @Test
    void updateSelf_字段为空不修改() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        User original = newUser(1L, "self", UserService.USER_TYPE_NORMAL, 1, now);
        original.setPassword(PasswordUtil.encrypt("old", "1",
                now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        StubUserMapper mapper = mapperWith(new StubUserMapper(), original);
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder().build();
        UserDTO dto = service.updateSelf(1L, request);

        assertEquals("self", mapper.lastUpdated.getDisplayName(), "空字段不应修改显示名");
        assertEquals(original.getPassword(), mapper.lastUpdated.getPassword(), "空字段不应修改密码");
        assertEquals("self", dto.getDisplayName());
    }

    @Test
    void updateSelf_不改变用户类型与enabled() {
        LocalDateTime now = LocalDateTime.now().withNano(0);
        StubUserMapper mapper = mapperWith(new StubUserMapper(),
                newUser(1L, "self", UserService.USER_TYPE_NORMAL, 0, now));
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder().displayName("改名").build();
        service.updateSelf(1L, request);

        assertEquals(UserService.USER_TYPE_NORMAL, mapper.lastUpdated.getUserType(),
                "自助修改不得改变用户类型");
        assertEquals(Integer.valueOf(0), mapper.lastUpdated.getEnabled(),
                "自助修改不得改变 enabled");
    }

    @Test
    void updateSelf_用户不存在抛USER_NOT_FOUND() {
        StubUserMapper mapper = new StubUserMapper();
        UserServiceImpl service = new UserServiceImpl(mapper);

        UserSelfUpdateRequest request = UserSelfUpdateRequest.builder().displayName("x").build();
        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateSelf(88L, request));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    // ================= 请求体字段约束（编译期保证 + 反射兜底） =================

    @Test
    void 请求体不包含userType与enabled字段() throws Exception {
        assertNoField(UserCreateRequest.class, "userType", "UserCreateRequest 不得包含 userType 字段");
        assertNoField(UserUpdateRequest.class, "userType", "UserUpdateRequest 不得包含 userType 字段");
        assertNoField(UserSelfUpdateRequest.class, "userType", "UserSelfUpdateRequest 不得包含 userType 字段");
        assertNoField(UserSelfUpdateRequest.class, "enabled", "UserSelfUpdateRequest 不得包含 enabled 字段（enabled 不可自助修改）");
    }

    private void assertNoField(Class<?> clazz, String fieldName, String message) throws Exception {
        for (Field f : clazz.getDeclaredFields()) {
            assertFalse(fieldName.equals(f.getName()), message);
        }
    }

    @Test
    void updateUser_请求体不再声明userType字段() {
        assertNull(getDeclaredFieldOrNull(UserUpdateRequest.class, "userType"),
                "UserUpdateRequest 不应再声明 userType 字段");
    }

    private Object getDeclaredFieldOrNull(Class<?> clazz, String name) {
        try {
            return clazz.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}