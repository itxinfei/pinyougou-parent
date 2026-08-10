package com.pinyougou.user.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.pojo.TbUserExample.Criteria;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private static final Logger logger = Logger.getLogger(UserDetailServiceImpl.class);

    @Autowired
    private TbUserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("经过认证类:" + username);

        // 从数据库查询用户
        TbUserExample example = new TbUserExample();
        Criteria criteria = example.createCriteria();
        criteria.andUsernameEqualTo(username);
        criteria.andStatusEqualTo("1"); // 只查询状态为正常的用户

        List<TbUser> userList = userMapper.selectByExample(example);

        if (userList.isEmpty()) {
            logger.warn("用户不存在: " + username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        TbUser user = userList.get(0);

        // 构建权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 返回UserDetails对象
        return new User(user.getUsername(), user.getPassword(), authorities);
    }
}
