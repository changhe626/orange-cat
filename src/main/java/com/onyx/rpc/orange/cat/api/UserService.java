package com.onyx.rpc.orange.cat.api;

import com.onyx.rpc.orange.cat.annotation.SPI;

/**
 * @author zk
 * @Description:
 * @date 2019-06-18 14:54
 */
@SPI("student")
public interface UserService {


    /**
     * 测试方法
     * @param name
     * @return
     */
    String hello(String name);


}
