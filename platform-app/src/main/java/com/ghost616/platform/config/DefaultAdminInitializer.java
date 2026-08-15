package com.ghost616.platform.config;

import com.ghost616.platform.entity.User;
import com.ghost616.platform.repository.UserMapper;
import com.ghost616.platform.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 默认管理员初始化器。
 *
 * <p>应用启动时若用户表为空，则插入默认管理员
 * （id=1、login_name=admin、display_name=管理员、user_type=2，
 * 密码为按加密公式 SM3(MD5(明文密码+用户ID)+创建时间毫秒时间戳) 加密后的 123456）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements ApplicationRunner {

    /** 默认管理员 ID。 */
    private static final long DEFAULT_ADMIN_ID = 1L;

    /** 默认管理员登录名。 */
    private static final String DEFAULT_ADMIN_LOGIN_NAME = "admin";

    /** 默认管理员显示名。 */
    private static final String DEFAULT_ADMIN_DISPLAY_NAME = "管理员";

    /** 默认管理员用户类型：管理员。 */
    private static final int DEFAULT_ADMIN_USER_TYPE = 2;

    /** 默认管理员初始密码。 */
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final UserMapper userMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }
        LocalDateTime createTime = LocalDateTime.now().withNano(0);
        long createTimeMillis = createTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        User admin = new User();
        admin.setId(DEFAULT_ADMIN_ID);
        admin.setLoginName(DEFAULT_ADMIN_LOGIN_NAME);
        admin.setDisplayName(DEFAULT_ADMIN_DISPLAY_NAME);
        admin.setUserType(DEFAULT_ADMIN_USER_TYPE);
        admin.setEnabled(1);
        admin.setCreateTime(createTime);
        admin.setPassword(PasswordUtil.encrypt(DEFAULT_ADMIN_PASSWORD,
                String.valueOf(DEFAULT_ADMIN_ID), createTimeMillis));

        userMapper.insert(admin);
        log.info("默认管理员初始化完成: id=1, loginName=admin, displayName=管理员");
    }
}
