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
import com.pinyougou.mapper.GenericMapper;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.pojo.TbUserExample.Criteria;
import com.pinyougou.service.BaseServiceImpl;
import com.pinyougou.user.service.UserService;

import entity.PageResult;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class UserServiceImpl extends BaseServiceImpl<TbUser> implements UserService {

    private static final Logger logger = Logger.getLogger(UserServiceImpl.class);

    // BCrypt加密器（替代MD5）
    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 短信发送频率限制配置
    private static final long SMS_SEND_INTERVAL = 60 * 1000; // 60秒
    private static final int SMS_SEND_LIMIT_PER_DAY = 10;      // 每天最多10次


    @Autowired
    private TbUserMapper userMapper;

    @Override
    protected GenericMapper<TbUser> getMapper() {
        return userMapper;
    }

    /**
     * 增加用户（自定义：密码加密、设置注册时间等）
     */
    @Override
    @Transactional
    public void add(TbUser user) {

        user.setCreated(new Date());//用户注册时间
        user.setUpdated(new Date());//修改时间
        user.setSourceType("1");//注册来源

        // 使用BCrypt加密密码（替代MD5）
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userMapper.insert(user);
    }

    /**
     * 条件查询带分页
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
     */
    @Override
    public void createSmsCode(final String phone) {
        try {
            // 频率限制检查
            String lastSendTime = (String) redisTemplate.boundHashOps("smscode:sendtime").get(phone);
            if (lastSendTime != null) {
                long lastTime = Long.parseLong(lastSendTime);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTime < SMS_SEND_INTERVAL) {
                    long remainSeconds = (SMS_SEND_INTERVAL - (currentTime - lastTime)) / 1000;
                    throw new ValidationException("验证码发送过于频繁，请" + remainSeconds + "秒后再试");
                }
            }

            // 检查今日发送次数
            String todayCountKey = "smscode:count:" + phone + ":" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
            Long todayCount = redisTemplate.boundValueOps(todayCountKey).increment(1);
            if (todayCount == 1) {
                redisTemplate.boundValueOps(todayCountKey).expireAt(
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date()) + " 23:59:59"
                    )
                );
            }
            if (todayCount > SMS_SEND_LIMIT_PER_DAY) {
                throw new ValidationException("今日验证码发送次数已达上限，请明天再试");
            }

            // 生成验证码
            final String smscode = String.format("%06d", new SecureRandom().nextInt(1000000));
            logger.info("验证码：" + smscode);

            // 存入Redis（5分钟过期）
            redisTemplate.boundHashOps("smscode").put(phone, smscode);
            redisTemplate.boundHashOps("smscode").expire(5, java.util.concurrent.TimeUnit.MINUTES);

            // 记录发送时间
            redisTemplate.boundHashOps("smscode:sendtime").put(phone, String.valueOf(System.currentTimeMillis()));

            // 发送短信消息
            jmsTemplate.send(smsDestination, new MessageCreator() {

                @Override
                public Message createMessage(Session session) throws JMSException {
                    MapMessage message = session.createMapMessage();
                    message.setString("mobile", phone);
                    message.setString("template_code", template_code);
                    message.setString("sign_name", sign_name);
                    Map map = new HashMap();
                    map.put("number", smscode);
                    message.setString("param", JSON.toJSONString(map));
                    return message;
                }
            });

            logger.info("短信验证码已发送: " + phone);

        } catch (ValidationException e) {
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
