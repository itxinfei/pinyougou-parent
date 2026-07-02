package com.pinyougou.user.testutil;

import com.pinyougou.pojo.TbUser;
import org.mockito.Mockito;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 用户服务测试基类
 * <p>
 * 功能说明：
 * - 提供测试用户数据的创建方法
 * - 提供Redis操作的Mock方法
 * - 提供密码验证的便捷方法
 * <p>
 * 使用方式：
 * - 子类继承此类，可直接调用提供的工具方法
 * - 所有方法均为protected，仅对子类可见
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class UserServiceTestBase {

    /**
     * 创建测试用户（默认数据）
     * <p>
     * 默认值：
     * - ID: 1
     * - 用户名: testuser
     * - 密码: encoded_password
     * - 手机号: 13800138000
     * - 状态: 1（正常）
     *
     * @return TbUser对象
     */
    protected TbUser createTestUser() {
        TbUser user = new TbUser();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setPhone("13800138000");
        user.setNickName("测试用户");
        user.setStatus("1");
        user.setCreated(new Date());
        user.setUpdated(new Date());
        return user;
    }

    /**
     * 创建测试用户（自定义数据）
     *
     * @param id       用户ID
     * @param username 用户名
     * @param password 密码
     * @param phone    手机号
     * @param status   状态（"1"-正常，"0"-禁用）
     * @return TbUser对象
     */
    protected TbUser createTestUser(Long id, String username, String password,
                                     String phone, String status) {
        TbUser user = new TbUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setNickName("测试用户" + username);
        user.setStatus(status);
        user.setCreated(new Date());
        user.setUpdated(new Date());
        user.setSourceType("1");
        return user;
    }

    /**
     * 创建注册用的测试用户
     * <p>
     * 与createTestUser不同，此方法不设置ID和时间字段
     * 用于测试用户注册流程（add方法会自动设置这些字段）
     *
     * @param username 用户名
     * @param password 密码（明文，add方法会加密）
     * @param phone    手机号
     * @return TbUser对象
     */
    protected TbUser createTestUserForRegister(String username, String password, String phone) {
        TbUser user = new TbUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setStatus("1");
        user.setSourceType("1");
        return user;
    }

    /**
     * Mock Redis Hash操作
     * <p>
     * 创建BoundHashOperations mock，并设置redisTemplate.boundHashOps返回该mock
     * <p>
     * 使用场景：测试需要Redis Hash操作的方法
     *
     * @param redisTemplate RedisTemplate对象
     * @return BoundHashOperations mock对象
     */
    @SuppressWarnings("unchecked")
    protected BoundHashOperations<String, Object, Object> mockHashOperations(RedisTemplate<String, Object> redisTemplate) {
        BoundHashOperations<String, Object, Object> hashOps =
            mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(hashOps);
        return hashOps;
    }

    /**
     * Mock短信验证码发送
     * <p>
     * 设置Redis中存储的验证码，用于测试checkSmsCode方法
     *
     * @param redisTemplate RedisTemplate对象
     * @param phone         手机号
     * @param code          验证码
     */
    @SuppressWarnings("unchecked")
    protected void mockSmsCodeSent(RedisTemplate<String, Object> redisTemplate, String phone, String code) {
        BoundHashOperations<String, Object, Object> hashOps =
            mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        when(hashOps.get(phone)).thenReturn(code);
    }

    /**
     * 验证BCrypt密码加密结果
     * <p>
     * 验证：
     * - 密码不为null
     * - 密码以$2a$开头（BCrypt格式）
     * - 密码长度为60字符
     *
     * @param password 加密后的密码
     */
    protected void verifyPasswordEncoded(String password) {
        assertNotNull("密码不应为null", password);
        assertTrue("密码应以$2a$开头", password.startsWith("$2a$"));
        assertEquals("密码长度应为60", 60, password.length());
    }
}
