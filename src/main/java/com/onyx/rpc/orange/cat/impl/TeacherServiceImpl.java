package com.onyx.rpc.orange.cat.impl;

import com.onyx.rpc.orange.cat.api.UserService;

/**
 * @author zk
 * @Description:
 * @date 2019-06-18 14:55
 */
public class TeacherServiceImpl implements UserService{

    @Override
    public String hello(String name) {
        return "teacher service:"+name;
    }
}
