package com.pinyougou.user.service.impl;

import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.user.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 用户服务实现类测试
 * <p>
 * 测试覆盖：
 * - 用户查询（findAll、findOne、findPage）
 * - 用户注册（add）
 * - 用户更新（update）
 * - 用户删除（delete）
 * - 短信验证码（createSmsCode、checkSmsCode）
 *
 * @author Administrator
 */
@RunWith(MockitoJUnitRunner.class)
public class UserServiceImplTest {

    @Mock
    private TbUserMapper userMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private Destination smsDestination;

    @InjectMocks
    private UserServiceImpl userServiceImpl;

    private TbUser testUser;

    /**
     * 测试前置准备
     */
    @Before
    public void setUp() {
        // 准备测试用户数据
        testUser = new TbUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encoded_password");
        testUser.setPhone("13800138000");
        testUser.setNickName("测试用户");
        testUser.setStatus("1");
        testUser.setCreated(new Date());
        testUser.setUpdated(new Date());
    }

    /**
     * 测试查询全部用户
     */
    @Test
    public void testFindAll() {
        // 准备测试数据
        List<TbUser> userList = new ArrayList<>();
        userList.add(testUser);

        // Mock行为
        Mockito.when(userMapper.selectByExample(null)).thenReturn(userList);

        // 执行测试
        List<TbUser> result = userServiceImpl.findAll();

        // 验证结果
        assertNotNull("用户列表不应为null", result);
        assertEquals("用户数量应为1", 1, result.size());
        assertEquals("用户名不匹配", testUser.getUsername(), result.get(0).getUsername());

        // 验证方法调用
        Mockito.verify(userMapper).selectByExample(null);
    }

    /**
     * 测试根据ID查询用户
     */
    @Test
    public void testFindOne() {
        Long userId = 1L;

        // Mock行为
        Mockito.when(userMapper.selectByPrimaryKey(userId)).thenReturn(testUser);

        // 执行测试
        TbUser result = userServiceImpl.findOne(userId);

        // 验证结果
        assertNotNull("用户不应为null", result);
        assertEquals("用户ID不匹配", userId, result.getId());

        // 验证方法调用
        Mockito.verify(userMapper).selectByPrimaryKey(userId);
    }

    /**
     * 测试用户注册
     */
    @Test
    public void testAdd() {
        // 准备新用户数据
        TbUser newUser = new TbUser();
        newUser.setUsername("newuser");
        newUser.setPassword("plain_password");
        newUser.setPhone("13900139000");
        newUser.setStatus("1");

        // Mock行为
        Mockito.when(userMapper.selectByExample(Mockito.any(TbUserExample.class))).thenReturn(new ArrayList<>());
        Mockito.doNothing().when(userMapper).insert(Mockito.any(TbUser.class));

        // 执行测试
        userServiceImpl.add(newUser);

        // 验证结果
        assertNotNull("创建时间不应为null", newUser.getCreated());
        assertNotNull("更新时间不应为null", newUser.getUpdated());
        assertEquals("注册来源应为1", "1", newUser.getSourceType());
        assertNotNull("密码应已加密", newUser.getPassword());
        assertNotEquals("密码不应是明文", "plain_password", newUser.getPassword());

        // 验证方法调用
        Mockito.verify(userMapper).insert(Mockito.any(TbUser.class));
    }

    /**
     * 测试用户更新
     */
    @Test
    public void testUpdate() {
        // Mock行为
        Mockito.doNothing().when(userMapper).updateByPrimaryKey(Mockito.any(TbUser.class));

        // 执行测试
        userServiceImpl.update(testUser);

        // 验证方法调用
        Mockito.verify(userMapper).updateByPrimaryKey(testUser);
    }

    /**
     * 测试批量删除用户
     */
    @Test
    public void testDelete() {
        Long[] ids = {1L, 2L, 3L};

        // Mock行为
        Mockito.doNothing().when(userMapper).deleteByPrimaryKey(Mockito.anyLong());

        // 执行测试
        userServiceImpl.delete(ids);

        // 验证方法调用
        Mockito.verify(userMapper, Mockito.times(3)).deleteByPrimaryKey(Mockito.anyLong());
    }

    /**
     * 测试短信验证码生成
     */
    @Test
    public void testCreateSmsCode() {
        String phone = "13800138000";

        // Mock Redis
        Mockito.when(redisTemplate.boundHashOps(Mockito.anyString())).thenReturn(null);

        // Mock JMS
        Mockito.doNothing().when(jmsTemplate).send(Mockito.any(Destination.class), Mockito.any());

        // 执行测试
        userServiceImpl.createSmsCode(phone);

        // 验证Redis写入
        Mockito.verify(redisTemplate).boundHashOps("smscode");
        Mockito.verify(redisTemplate).boundHashOps("smscode").put(Mockito.eq(phone), Mockito.anyString());

        // 验证JMS发送
        Mockito.verify(jmsTemplate).send(Mockito.eq(smsDestination), Mockito.any());
    }

    /**
     * 测试短信验证码校验（成功）
     */
    @Test
    public void testCheckSmsCodeSuccess() {
        String phone = "13800138000";
        String correctCode = "123456";

        // Mock Redis返回正确的验证码
        Mockito.when(redisTemplate.boundHashOps("smscode").get(phone)).thenReturn(correctCode);

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, correctCode);

        // 验证结果
        assertTrue("验证码校验应成功", result);
    }

    /**
     * 测试短信验证码校验（失败）
     */
    @Test
    public void testCheckSmsCodeFailure() {
        String phone = "13800138000";
        String wrongCode = "654321";

        // Mock Redis返回正确的验证码
        Mockito.when(redisTemplate.boundHashOps("smscode").get(phone)).thenReturn("123456");

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, wrongCode);

        // 验证结果
        assertFalse("验证码校验应失败", result);
    }

    /**
     * 测试短信验证码校验（验证码不存在）
     */
    @Test
    public void testCheckSmsCodeNotFound() {
        String phone = "13800138000";
        String code = "123456";

        // Mock Redis返回null
        Mockito.when(redisTemplate.boundHashOps("smscode").get(phone)).thenReturn(null);

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, code);

        // 验证结果
        assertFalse("验证码不存在时应返回false", result);
    }

    /**
     * 测试BCrypt密码加密
     */
    @Test
    public void testBCryptPasswordEncoding() {
        // 准备用户
        TbUser user = new TbUser();
        user.setUsername("testuser");
        user.setPassword("myPassword123");

        // Mock数据库查询返回空（用户不存在）
        Mockito.when(userMapper.selectByExample(Mockito.any(TbUserExample.class))).thenReturn(new ArrayList<>());
        Mockito.doNothing().when(userMapper).insert(Mockito.any(TbUser.class));

        // 执行注册
        userServiceImpl.add(user);

        // 验证密码已加密
        String encodedPassword = user.getPassword();
        assertNotNull("密码不应为null", encodedPassword);
        assertNotEquals("密码不应是明文", "myPassword123", encodedPassword);

        // BCrypt密码格式：$2a$10$...（60字符）
        assertTrue("密码应以$2a$开头", encodedPassword.startsWith("$2a$"));
        assertEquals("密码长度应为60", 60, encodedPassword.length());
    }
}
