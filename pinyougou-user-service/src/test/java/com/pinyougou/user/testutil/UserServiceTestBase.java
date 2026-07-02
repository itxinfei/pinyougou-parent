package com.pinyougou.user.testutil;

import com.pinyougou.pojo.TbUser;
import org.mockito.Mockito;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class UserServiceTestBase {

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

    protected TbUser createTestUserForRegister(String username, String password, String phone) {
        TbUser user = new TbUser();
        user.setUsername(username);
        user.setPassword(password);
        user.setPhone(phone);
        user.setStatus("1");
        user.setSourceType("1");
        return user;
    }

    @SuppressWarnings("unchecked")
    protected BoundHashOperations<String, Object, Object> mockHashOperations(RedisTemplate<String, Object> redisTemplate) {
        BoundHashOperations<String, Object, Object> hashOps =
            mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(hashOps);
        return hashOps;
    }

    @SuppressWarnings("unchecked")
    protected void mockSmsCodeSent(RedisTemplate<String, Object> redisTemplate, String phone, String code) {
        BoundHashOperations<String, Object, Object> hashOps =
            mock(BoundHashOperations.class);
        when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        when(hashOps.get(phone)).thenReturn(code);
    }

    protected void verifyPasswordEncoded(String password) {
        assertNotNull("密码不应为null", password);
        assertTrue("密码应以$2a$开头", password.startsWith("$2a$"));
        assertEquals("密码长度应为60", 60, password.length());
    }
}
