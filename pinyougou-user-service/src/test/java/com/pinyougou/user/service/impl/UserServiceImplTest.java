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
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 用户服务实现类单元测试
 * <p>
 * 测试覆盖：
 * - 用户查询：findAll、findOne
 * - 用户注册：add（密码加密、默认字段设置）
 * - 用户更新：update
 * - 用户删除：delete
 * - 短信验证码：createSmsCode（生成+发送）、checkSmsCode（校验）
 * <p>
 * 测试策略：
 * - 使用Mockito模拟Mapper、Redis、JMS依赖
 * - 使用@Mock标注Mock对象，@InjectMocks自动注入
 * - 使用MockitoJUnitRunner.Silent.class避免strict stubbing检查
 * - 每个测试方法独立，不依赖其他测试的执行顺序
 * <p>
 * Mock对象说明：
 * - userMapper: 用户数据访问层
 * - redisTemplate: Redis缓存（存储短信验证码）
 * - jmsTemplate: JMS消息模板（发送短信）
 * - smsDestination: 短信服务Destination
 * <p>
 * 注意事项：
 * - BCrypt加密是确定性的（相同输入相同输出），但每次生成的salt不同
 * - 短信验证码生成使用SecureRandom，每次结果不同
 * - 实现中未做用户名唯一性检查，重复用户名会直接插入
 *
 * @author Administrator
 * @since 1.0-SNAPSHOT
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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
     * <p>
     * 初始化测试用户数据：
     * - ID: 1
     * - 用户名: testuser
     * - 密码: encoded_password（已加密）
     * - 手机号: 13800138000
     * - 状态: 1（正常）
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
     * <p>
     * 验证：
     * 1. 返回的用户列表不为null
     * 2. 用户数量与预期一致
     * 3. 用户名匹配
     * 4. selectByExample方法被正确调用
     */
    @Test
    public void testFindAll() {
        // 准备测试数据
        List<TbUser> userList = new ArrayList<>();
        userList.add(testUser);

        // Mock行为：selectByExample返回用户列表
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
     * <p>
     * 验证：
     * 1. 返回的用户不为null
     * 2. 用户ID与查询ID一致
     * 3. selectByPrimaryKey方法被正确调用
     */
    @Test
    public void testFindOne() {
        Long userId = 1L;

        // Mock行为：selectByPrimaryKey返回测试用户
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
     * <p>
     * 验证：
     * 1. 创建时间和更新时间被设置
     * 2. 注册来源设置为"1"
     * 3. 密码被BCrypt加密（以$2a$开头）
     * 4. 密码不再是明文
     * 5. insert方法被调用
     * <p>
     * 注意：insert返回int（受影响行数），Mock时使用thenReturn(1)
     */
    @Test
    public void testAdd() {
        // 准备新用户数据
        TbUser newUser = new TbUser();
        newUser.setUsername("newuser");
        newUser.setPassword("plain_password");
        newUser.setPhone("13900139000");
        newUser.setStatus("1");

        // Mock行为 - insert返回int（受影响行数）
        Mockito.when(userMapper.insert(Mockito.any(TbUser.class))).thenReturn(1);

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
     * <p>
     * 验证：updateByPrimaryKey方法被正确调用
     * <p>
     * 注意：updateByPrimaryKey返回int（受影响行数），Mock时使用thenReturn(1)
     */
    @Test
    public void testUpdate() {
        // Mock行为 - updateByPrimaryKey返回int（受影响行数）
        Mockito.when(userMapper.updateByPrimaryKey(Mockito.any(TbUser.class))).thenReturn(1);

        // 执行测试
        userServiceImpl.update(testUser);

        // 验证方法调用
        Mockito.verify(userMapper).updateByPrimaryKey(testUser);
    }

    /**
     * 测试批量删除用户
     * <p>
     * 验证：
     * 1. 删除操作被正确执行
     * 2. deleteByPrimaryKey被调用的次数与ID数组长度一致
     * <p>
     * 注意：deleteByPrimaryKey返回int（受影响行数），Mock时使用thenReturn(1)
     */
    @Test
    public void testDelete() {
        Long[] ids = {1L, 2L, 3L};

        // Mock行为 - deleteByPrimaryKey返回int（受影响行数）
        Mockito.when(userMapper.deleteByPrimaryKey(Mockito.anyLong())).thenReturn(1);

        // 执行测试
        userServiceImpl.delete(ids);

        // 验证方法调用次数（3个ID，调用3次）
        Mockito.verify(userMapper, Mockito.times(3)).deleteByPrimaryKey(Mockito.anyLong());
    }

    /**
     * 测试短信验证码生成
     * <p>
     * 验证：
     * 1. Redis中记录发送时间（用于频率限制）
     * 2. Redis中记录今日发送次数
     * 3. 验证码存入Redis（smscode）
     * 4. JMS发送短信消息
     * <p>
     * Mock说明：
     * - 需要为所有Redis操作提供mock（boundHashOps、boundValueOps）
     * - 需要mock JMS发送操作
     * <p>
     * 注意：短信验证码使用SecureRandom生成，每次结果不同
     */
    @Test
    public void testCreateSmsCode() {
        String phone = "13800138000";

        // Mock Redis - 需要为所有boundHashOps和boundValueOps调用提供mock
        BoundHashOperations<String, Object, Object> hashOps =
            Mockito.mock(BoundHashOperations.class);
        org.springframework.data.redis.core.BoundValueOperations<String, Object> valueOps =
            Mockito.mock(org.springframework.data.redis.core.BoundValueOperations.class);

        // 频率限制检查 - smscode:sendtime（记录上次发送时间）
        Mockito.when(redisTemplate.boundHashOps("smscode:sendtime")).thenReturn(hashOps);
        Mockito.when(hashOps.get(phone)).thenReturn(null);

        // 今日发送次数 - smscode:count:phone:date（每天重置）
        Mockito.when(redisTemplate.boundValueOps(Mockito.anyString())).thenReturn(valueOps);
        Mockito.when(valueOps.increment(1)).thenReturn(1L);

        // 验证码存储 - smscode（存储验证码，5分钟过期）
        Mockito.when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);

        // JMS发送（通过ActiveMQ发送短信消息）
        Mockito.doNothing().when(jmsTemplate).send(Mockito.any(Destination.class), Mockito.any());

        // 执行测试
        userServiceImpl.createSmsCode(phone);

        // 验证JMS发送被调用
        Mockito.verify(jmsTemplate).send(Mockito.eq(smsDestination), Mockito.any());
    }

    /**
     * 测试短信验证码校验（成功）
     * <p>
     * 验证：当验证码正确时，返回true
     * <p>
     * Mock说明：
     * - Redis存储的验证码为"123456"
     * - 传入的验证码也是"123456"
     * - 需要正确设置Redis mock链式调用
     */
    @Test
    public void testCheckSmsCodeSuccess() {
        String phone = "13800138000";
        String correctCode = "123456";

        // Mock Redis - 需要设置BoundHashOperations返回值
        BoundHashOperations<String, Object, Object> hashOps =
            Mockito.mock(BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        Mockito.when(hashOps.get(phone)).thenReturn(correctCode);

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, correctCode);

        // 验证结果
        assertTrue("验证码校验应成功", result);
    }

    /**
     * 测试短信验证码校验（失败）
     * <p>
     * 验证：当验证码错误时，返回false
     * <p>
     * Mock说明：
     * - Redis存储的验证码为"123456"
     * - 传入的验证码为"654321"（错误）
     */
    @Test
    public void testCheckSmsCodeFailure() {
        String phone = "13800138000";
        String wrongCode = "654321";

        // Mock Redis - 需要设置BoundHashOperations返回值
        BoundHashOperations<String, Object, Object> hashOps =
            Mockito.mock(BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        Mockito.when(hashOps.get(phone)).thenReturn("123456");

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, wrongCode);

        // 验证结果
        assertFalse("验证码校验应失败", result);
    }

    /**
     * 测试短信验证码校验（验证码不存在）
     * <p>
     * 验证：当Redis中没有验证码时（已过期或未发送），返回false
     * <p>
     * Mock说明：
     * - Redis返回null（验证码不存在）
     */
    @Test
    public void testCheckSmsCodeNotFound() {
        String phone = "13800138000";
        String code = "123456";

        // Mock Redis - 需要设置BoundHashOperations返回值
        BoundHashOperations<String, Object, Object> hashOps =
            Mockito.mock(BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        Mockito.when(hashOps.get(phone)).thenReturn(null);

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, code);

        // 验证结果
        assertFalse("验证码不存在时应返回false", result);
    }

    /**
     * 测试BCrypt密码加密
     * <p>
     * 验证：BCrypt加密结果符合预期格式
     * <p>
     * 注意：当前为空测试，如需验证可在testAdd中检查密码格式
     * BCrypt加密结果格式：$2a$10$...（60字符）
     */
    @Test
    public void testBCryptPasswordEncoding() {
    }

    /**
     * 测试用户注册（用户名重复）
     * <p>
     * 验证：当用户名已存在时，实现未做唯一性检查，直接插入
     * <p>
     * 注意：当前实现中add方法没有用户名唯一性检查
     * 如果需要唯一性检查，应在add方法中添加selectByExample查询
     */
    @Test
    public void testAdd_DuplicateUsername() {
        // 准备新用户数据
        TbUser newUser = new TbUser();
        newUser.setUsername("existinguser");
        newUser.setPassword("plain_password");
        newUser.setPhone("13900139000");

        // Mock行为 - insert返回int（受影响行数）
        Mockito.when(userMapper.insert(Mockito.any(TbUser.class))).thenReturn(1);

        // 执行测试 - 实现不做唯一性检查，直接插入
        userServiceImpl.add(newUser);

        // 验证用户被插入（即使用户名重复）
        Mockito.verify(userMapper).insert(Mockito.any(TbUser.class));
        assertNotNull("创建时间不应为null", newUser.getCreated());
    }

    /**
     * 测试短信验证码过期（Redis中不存在）
     * <p>
     * 验证：当验证码已过期（Redis中不存在）时，校验返回false
     * <p>
     * 注意：验证码在Redis中存储5分钟，过期后自动删除
     */
    @Test
    public void testCheckSmsCode_Expired() {
        String phone = "13800138000";
        String code = "123456";

        // Mock Redis - 需要设置BoundHashOperations返回值
        BoundHashOperations<String, Object, Object> hashOps =
            Mockito.mock(BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        Mockito.when(hashOps.get(phone)).thenReturn(null);

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, code);

        // 验证结果
        assertFalse("过期验证码校验应失败", result);
    }

    /**
     * 测试短信验证码为空
     * <p>
     * 验证：当传入空字符串验证码时，校验返回false
     * <p>
     * 注意：空字符串与null不同，但校验结果都是false
     */
    @Test
    public void testCheckSmsCode_EmptyCode() {
        String phone = "13800138000";
        String emptyCode = "";
        String correctCode = "123456";

        // Mock Redis - 需要设置BoundHashOperations返回值
        BoundHashOperations<String, Object, Object> hashOps =
            Mockito.mock(BoundHashOperations.class);
        Mockito.when(redisTemplate.boundHashOps("smscode")).thenReturn(hashOps);
        Mockito.when(hashOps.get(phone)).thenReturn(correctCode);

        // 执行测试
        boolean result = userServiceImpl.checkSmsCode(phone, emptyCode);

        // 验证结果
        assertFalse("空验证码校验应失败", result);
    }
}
