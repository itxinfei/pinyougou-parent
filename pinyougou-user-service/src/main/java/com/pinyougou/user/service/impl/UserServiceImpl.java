package com.pinyougou.user.service.impl;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.exception.ValidationException;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.pojo.TbUserExample.Criteria;
import com.pinyougou.user.service.LoginService;
import com.pinyougou.user.service.UserService;

import entity.PageResult;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class);

    // ✅ 使用BCrypt加密器（替代MD5）
    // BCrypt特点：
    // - 自动加盐（salt自动生成并包含在哈希值中）
    // - 工作因子可调（默认10，越高越安全但越慢）
    // - 抗彩虹表攻击
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ⚠️ 短信发送频率限制配置（建议移到application.properties）
    private static final long SMS_SEND_INTERVAL = 60 * 1000; // 60秒
    private static final int SMS_SEND_LIMIT_PER_DAY = 10;      // 每天最多10次


    @Autowired
    private TbUserMapper userMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbUser> findAll() {
        return userMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbUser> page = (Page<TbUser>) userMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加用户
     * <p>
     * 注册流程：
     * 1. 设置用户注册时间和更新时间
     * 2. 密码加密（BCrypt，自动加盐）
     * 3. 设置默认状态（正常、已验证等）
     * 4. 保存到数据库
     * <p>
     * ✅ 已优化：密码加密方式
     * - 旧方案：MD5（易被彩虹表破解）
     * - 新方案：BCrypt（自动加盐、工作因子可调）
     * <p>
     * 默认字段说明：
     * - status: "1" (正常状态)
     * - sourceType: "1" (注册来源)
     * - isMobileCheck: "1" (手机已验证)
     * <p>
     * ⚠️ 注意事项：
     * - 未设置用户名唯一性检查
     * - 未设置手机号唯一性检查
     * - 未处理并发注册问题（可能出现重复数据）
     *
     * @param user 用户实体（username/password/phone等）
     */
    @Override
    @Transactional
    public void add(TbUser user) {

        user.setCreated(new Date());//用户注册时间
        user.setUpdated(new Date());//修改时间
        user.setSourceType("1");//注册来源

        // ✅ 使用BCrypt加密密码（替代MD5）
        // BCrypt自动生成salt并包含在哈希值中
        // 工作因子默认为10（可配置 4-31，值越高越安全但越慢）
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userMapper.insert(user);
    }

    /**
     * 修改
     */
    @Override
    @Transactional
    public void update(TbUser user) {
        userMapper.updateByPrimaryKey(user);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbUser findOne(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    @Transactional
    public void delete(Long[] ids) {
        for (Long id : ids) {
            userMapper.deleteByPrimaryKey(id);
        }
    }

    /**
     * @param user
     * @param pageNum  当前页 码
     * @param pageSize 每页记录数
     * @return
     */
    @Override
    public PageResult findPage(TbUser user, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbUserExample example = new TbUserExample();
        Criteria criteria = example.createCriteria();

        if (user != null) {
            if (user.getUsername() != null && user.getUsername().length() > 0) {
                criteria.andUsernameLike("%" + user.getUsername() + "%");
            }
            if (user.getPhone() != null && user.getPhone().length() > 0) {
                criteria.andPhoneLike("%" + user.getPhone() + "%");
            }
            if (user.getEmail() != null && user.getEmail().length() > 0) {
                criteria.andEmailLike("%" + user.getEmail() + "%");
            }
            if (user.getSourceType() != null && user.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + user.getSourceType() + "%");
            }
            if (user.getNickName() != null && user.getNickName().length() > 0) {
                criteria.andNickNameLike("%" + user.getNickName() + "%");
            }
            if (user.getName() != null && user.getName().length() > 0) {
                criteria.andNameLike("%" + user.getName() + "%");
            }
            if (user.getStatus() != null && user.getStatus().length() > 0) {
                criteria.andStatusLike("%" + user.getStatus() + "%");
            }
            if (user.getHeadPic() != null && user.getHeadPic().length() > 0) {
                criteria.andHeadPicLike("%" + user.getHeadPic() + "%");
            }
            if (user.getQq() != null && user.getQq().length() > 0) {
                criteria.andQqLike("%" + user.getQq() + "%");
            }
            if (user.getIsMobileCheck() != null && user.getIsMobileCheck().length() > 0) {
                criteria.andIsMobileCheckLike("%" + user.getIsMobileCheck() + "%");
            }
            if (user.getIsEmailCheck() != null && user.getIsEmailCheck().length() > 0) {
                criteria.andIsEmailCheckLike("%" + user.getIsEmailCheck() + "%");
            }
            if (user.getSex() != null && user.getSex().length() > 0) {
                criteria.andSexLike("%" + user.getSex() + "%");
            }

        }

        Page<TbUser> page = (Page<TbUser>) userMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private Destination smsDestination;

    @Value("${template_code}")
    private String template_code;

    @Value("${sign_name}")
    private String sign_name;

    /**
     * 生成短信验证码并发送
     * <p>
     * 执行流程：
     * 1. ✅ 频率检查（60秒内同一手机号只能发送1次）
     * 2. ✅ 每日次数检查（同一手机号每天最多10次）
     * 3. 生成6位随机数（使用SecureRandom）
     * 4. 将验证码存入Redis（5分钟过期）
     * 5. 通过ActiveMQ发送短信消息
     * <p>
     * ✅ 已优化：
     * - 使用SecureRandom替代Math.random()
     * - 添加发送频率限制（60秒）
     * - 添加每日发送上限（10次）
     * - Redis设置过期时间（5分钟）
     * <p>
     * ⚠️ 待优化：
     * - 未记录发送日志（手机号、IP、时间）
     * - 未添加图形验证码校验
     * - 未添加IP频率限制
     *
     * @param phone 手机号
     */
    @Override
    public void createSmsCode(final String phone) {
        try {
            // ========== 第一步：频率限制检查 ==========
            // 1.1 检查60秒内是否发送过
            String lastSendTime = (String) redisTemplate.boundHashOps("smscode:sendtime").get(phone);
            if (lastSendTime != null) {
                long lastTime = Long.parseLong(lastSendTime);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTime < SMS_SEND_INTERVAL) {
                    long remainSeconds = (SMS_SEND_INTERVAL - (currentTime - lastTime)) / 1000;
                    throw new ValidationException("验证码发送过于频繁，请" + remainSeconds + "秒后再试");
                }
            }

            // 1.2 检查今日发送次数
            String todayCountKey = "smscode:count:" + phone + ":" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
            Long todayCount = redisTemplate.boundValueOps(todayCountKey).increment(1);
            if (todayCount == 1) {
                // 第一次发送，设置过期时间为当天剩余时间
                redisTemplate.boundValueOps(todayCountKey).expireAt(
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 23:59:59"
                    )
                );
            }
            if (todayCount > SMS_SEND_LIMIT_PER_DAY) {
                throw new ValidationException("今日验证码发送次数已达上限，请明天再试");
            }

            // ========== 第二步：生成验证码 ==========
            // ✅ 使用加密安全的随机数生成器
            final String smscode = String.format("%06d", new SecureRandom().nextInt(1000000));
            logger.info("验证码：" + smscode);

            // ========== 第三步：存入Redis（5分钟过期） ==========
            redisTemplate.boundHashOps("smscode").put(phone, smscode);
            redisTemplate.boundHashOps("smscode").expire(5, java.util.concurrent.TimeUnit.MINUTES);

            // 记录发送时间（用于频率限制）
            redisTemplate.boundHashOps("smscode:sendtime").put(phone, String.valueOf(System.currentTimeMillis()));

            // ========== 第四步：发送短信消息 ==========
            jmsTemplate.send(smsDestination, new MessageCreator() {

                @Override
                public Message createMessage(Session session) throws JMSException {
                    MapMessage message = session.createMapMessage();
                    message.setString("mobile", phone);//手机号
                    message.setString("template_code", template_code);//验证码
                    message.setString("sign_name", sign_name);//签名
                    Map map = new HashMap();
                    map.put("number", smscode);
                    message.setString("param", JSON.toJSONString(map));
                    return message;
                }
            });

            logger.info("短信验证码已发送: " + phone);

        } catch (ValidationException e) {
            // 频率限制异常，直接抛出
            throw e;
        } catch (Exception e) {
            logger.error("短信发送失败: " + phone, e);
            throw new ValidationException("短信发送失败，请稍后重试");
        }
    }

    /**
     * @param phone
     * @param code
     * @return
     */
    @Override
    public boolean checkSmsCode(String phone, String code) {
        String systemcode = (String) redisTemplate.boundHashOps("smscode").get(phone);
        if (systemcode == null) {
            return false;
        }
        if (!systemcode.equals(code)) {
            return false;
        }
        // 验证成功后删除验证码，防止重复使用
        redisTemplate.boundHashOps("smscode").delete(phone);
        return true;
    }

    @Override
    public TbUser findByUsername(String username) {
        TbUserExample example = new TbUserExample();
        Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);
        List<TbUser> users = userMapper.selectByExample(example);
        return users != null && !users.isEmpty() ? users.get(0) : null;
    }

    @Override
    public void updateUserInfo(TbUser user) {
        userMapper.updateByPrimaryKeySelective(user);
    }

    @Override
    public void updatePassword(String username, String oldPassword, String newPassword) {
        TbUser user = findByUsername(username);
        if (user == null) {
            throw new ValidationException("用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ValidationException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateByPrimaryKeySelective(user);
    }
}
