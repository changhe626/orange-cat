package com.onyx.rpc.orange.cat.service.impl;

import com.onyx.rpc.orange.cat.service.UserService;

/**
 * @author zk
 * @Description:
 * @date 2019-06-18 14:55
 */
public class StudentServiceImpl  implements UserService{


    @Override
    public String hello(String name) {
        return "student service:"+name;
    }
}
