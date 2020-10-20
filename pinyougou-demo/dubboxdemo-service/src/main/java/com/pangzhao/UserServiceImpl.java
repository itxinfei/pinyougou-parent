package com.pangzhao;

import com.alibaba.dubbo.config.annotation.Service;

/**
 * 用户实现类
 */
@Service //alibaba
public class UserServiceImpl implements UserService {
    @Override
    public String getName() {
        //发布服务
        return "发布服务";
    }
}
