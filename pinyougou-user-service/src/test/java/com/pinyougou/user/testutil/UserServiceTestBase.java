package com.pinyougou.user.testutil;

import com.pinyougou.pojo.TbUser;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl测试工具类
 * <p>
 * 提供用户服务测试的公共方法：
 * 1. 创建测试用的用户数据
 * 2. Mock Redis和JMS的常用操作
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
public abstract class UserServiceTestBase {

    /**
     * 创建测试用户（默认数据）
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
     * @param id 用户ID
     * @param username 用户名
     * @param password 密码（明文）
     * @param phone 手机号
     * @param status 状态
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
     * 创建测试用户（注册场景）
     *
     * @param username 用户名
     * @param password 密码（明文，将被BCrypt加密）
     * @param phone 手机号
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
     * Mock Redis HashOperations
     *
     * @param redisTemplate RedisTemplate
     * @return HashOperations mock对象
     */
    protected org.springframework.data.redis.core.HashOperations mockHashOperations(RedisTemplate<String, Object> redisTemplate) {
        org.springframework.data.redis.core.HashOperations hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(hashOps);
        return hashOps;
    }

    /**
     * 模拟验证码已发送（存储到Redis）
     *
     * @param redisTemplate RedisTemplate
     * @param phone 手机号
     * @param code 验证码
     */
    protected void mockSmsCodeSent(RedisTemplate<String, Object> redisTemplate, String phone, String code) {
        org.springframework.data.redis.core.HashOperations hashOps =
            mock(org.springframework.data.redis.core.HashOperations.class);
        when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        when(hashOps.get(phone)).thenReturn(code);
    }

    /**
     * 验证密码已加密（BCrypt格式）
     *
     * @param password 密码
     */
    protected void verifyPasswordEncoded(String password) {
        assertNotNull("密码不应为null", password);
        assertNotEquals("密码不应是明文", password, password);
        assertTrue("密码应以$2a$开头", password.startsWith("$2a$"));
        assertEquals("密码长度应为60", 60, password.length());
    }
}
