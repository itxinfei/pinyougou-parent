package com.pinyougou.user.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.pojo.TbUser;

/**
 * Spring Security 用户认证服务
 * 通过 Dubbo 远程调用 UserService 获取用户信息
 */
@Service
public class UserDetailServiceImpl implements UserDetailsService {

    private static final Logger logger = Logger.getLogger(UserDetailServiceImpl.class);

    @Reference
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("经过认证类:" + username);

        // 通过 Dubbo 远程查询用户
        TbUser user = userService.findByUsername(username);

        if (user == null) {
            logger.warn("用户不存在: " + username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        if (!"1".equals(user.getStatus())) {
            logger.warn("用户已被禁用: " + username);
            throw new UsernameNotFoundException("用户已被禁用: " + username);
        }

        // 构建权限列表
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        // 返回UserDetails对象
        return new User(user.getUsername(), user.getPassword(), authorities);
    }
}
