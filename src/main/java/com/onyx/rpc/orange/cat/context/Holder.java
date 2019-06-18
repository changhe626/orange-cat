package com.onyx.rpc.orange.cat.context;


/**
 * @author zk
 * @Description: 自定义容器
 * @date 2019-06-18 14:58
 */
public class Holder<T> {

    private volatile T t;

    public T get() {
        return t;
    }

    public void set(T t) {
        this.t = t;
    }

    public Holder(T t) {
        this.t = t;
    }

    public Holder() {
    }
}
