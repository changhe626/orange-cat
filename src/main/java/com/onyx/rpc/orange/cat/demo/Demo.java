package com.onyx.rpc.orange.cat.demo;

import com.onyx.rpc.orange.cat.loader.ExtensionLoader;
import com.onyx.rpc.orange.cat.service.UserService;

import java.util.Optional;

/**
 * @author zk
 * @Description:
 * @date 2019-06-18 16:05
 */
public class Demo {


    public static void main(String[] args) {
        //获取默认实现类
        Optional<UserService> optional = ExtensionLoader.
                getExtensionLoader(UserService.class).
                getDefaultExtensionInstance();
        System.out.println(optional.get().hello("1"));


        //指定特定的实现类,例如配置的tobyLog
        Optional<UserService> tobyLog = ExtensionLoader.
                getExtensionLoader(UserService.class).
                getExtensionInstance("teacher");
        System.out.println(tobyLog.get().hello("2"));

    }

}
