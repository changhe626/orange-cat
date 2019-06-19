package com.onyx.rpc.orange.cat.loader;

import com.onyx.rpc.orange.cat.annotation.SPI;
import com.onyx.rpc.orange.cat.context.Holder;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * @author zk
 * @Description: 扩展加载器
 * @date 2019-06-18 15:04
 */
public class ExtensionLoader<T> {


    /**
     * 读取文件默认编码方式
     */
    private final static String CHARSET = "utf-8";

    /**
     * true 字符串
     */
    private final static String CHAR_TRUE = "true";

    /**
     * 分割SPI上默认拓展点字符串用的
     */
    private final static Pattern KEY_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /**
     * 定义SPI文件的扫描路径,dubbo源码中设置了多个,我们这里只设置一个路径就够了
     */
    private final static String DIRECTORY = "META-INF//OrangeCat/";

    /**
     * 保存所有的加载点
     */
    private final static ConcurrentHashMap<Class, ExtensionLoader> EXTENSION_LOADERS = new ConcurrentHashMap<Class, ExtensionLoader>();

    /**
     * 拓展点的缓存
     */
    private final static ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    /**
     * 传入的接口
     */
    private Class<T> clazz;

    /**
     * 接口SPI默认的实现名(就是把接口上填的默认值)
     */
    private String defaultKey;

    /**
     * 类名的缓存
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    /**
     * 反射出来的实例的缓存
     */
    private final ConcurrentMap<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();


    /**
     * @param clazz 接口名
     * @return 当前接口的加载扩展对象实例
     */
    public static <T> ExtensionLoader getExtensionLoader(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("接口不能为空");
        }
        if (!clazz.isInterface()) {
            throw new IllegalArgumentException("只能是接口");
        }
        if (!clazz.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("接口必须包含注解:" + SPI.class.getSimpleName() + " !");
        }
        //从缓存EXTENSION_LOADERS中获取,如果不存在则新建后加入缓存
        //对于每一个拓展,都会有且只有一个ExtensionLoader与其对应
        ExtensionLoader loader = EXTENSION_LOADERS.get(clazz);
        if (loader == null) {
            loader = new ExtensionLoader(clazz);
            EXTENSION_LOADERS.putIfAbsent(clazz, loader);
        }
        return loader;
    }


    /**
     * 获取默认的实现类对象
     *
     * @return
     */
    public Optional<T> getDefaultExtensionInstance() {
        getExtensionClasses();
        if (null == defaultKey || defaultKey.length() == 0 || CHAR_TRUE.equals(defaultKey)) {
            return Optional.empty();
        }
        return getExtensionInstance(defaultKey);
    }


    /**
     * 根据名字获取实现类对象
     *
     * @param key 名字,传递true字符串就是默认的
     */
    public Optional<T> getExtensionInstance(String key) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Extension key 不能为空");
        }
        if (CHAR_TRUE.equals(key)) {
            return getDefaultExtensionInstance();
        }
        Object instance = null;
        Holder<Object> holder = cachedInstances.get(key);
        if (holder != null) {
            instance = holder.get();
            if (instance == null) {
                instance = getInstance(key);
            }
        } else {
            instance = getInstance(key);
        }
        return Optional.of((T) instance);
    }


    /**
     * @param key key
     * @return 具体的实例对象
     */
    private synchronized Object getInstance(String key) {
        //获取对象
        Object instance = createExtensionInstance(key);
        //添加缓存
        cachedInstances.putIfAbsent(key, new Holder<Object>(instance));
        return instance;
    }


    /**
     * 获取所有加载的类
     *
     * @return
     */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }


    /**
     * 1.设置接口默认的实现类名  2.加载文件
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        SPI spi = clazz.getAnnotation(SPI.class);
        if (spi != null) {
            String value = spi.value();
            if (StringUtils.isNotBlank(value)) {
                String[] names = KEY_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("多个默认的实现 " + clazz.getName() + ": " + Arrays.toString(names));
                }
                if (names.length == 1) {
                    defaultKey = names[0];
                }
            }
        }
        Map<String, Class<?>> extensionClasses = loadFile();
        return extensionClasses;
    }


    /**
     * 加载解析spi配置文件,然后加入缓存
     */
    private Map<String, Class<?>> loadFile() {
        //拼接文件名字
        String fileName = DIRECTORY + clazz.getName();
        ClassLoader classLoader = getClassLoader();
        Enumeration<URL> urls = getUrlEnumeration(fileName, classLoader);
        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        if (urls != null) {
            while (urls.hasMoreElements()) {
                try {
                    URL url = urls.nextElement();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), CHARSET));
                    try {
                        String line = null;
                        while (StringUtils.isNotBlank(line = reader.readLine())) {
                            //包含#的这是注释,直接跳过
                            if (line.contains("#")) {
                                continue;
                            }
                            line = StringUtils.trim(line);
                            if (line.length() > 0) {
                                try {
                                    String key = null;
                                    int i = line.indexOf('=');
                                    if (i > 0) {
                                        //key
                                        key = StringUtils.trim(line.substring(0, i));
                                        //类全名
                                        line = StringUtils.trim(line.substring(i + 1));
                                    }
                                    if (line.length() > 0) {
                                        //加载类，并通过 loadClass 方法对类进行缓存
                                        Class<?> instance = Class.forName(line, true, classLoader);
                                        if (!clazz.isAssignableFrom(instance)) {
                                            throw new IllegalStateException("实例化" + line + "发生错误");
                                        }
                                        //加入缓存
                                        extensionClasses.put(key, instance);
                                    }
                                } catch (Exception e) {
                                    throw new IllegalStateException("实例化类发生错误(接口: " + clazz + ", class line: " + line + ") in " + url + ", 原因: " + e.getMessage(), e);
                                }
                            }
                        } //读取文件行结束
                    } finally {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return extensionClasses;
    }


    /**
     * 根据获取到的拓展点class实例化成对象返回
     *
     * @param key
     */
    private Object createExtensionInstance(String key) {
        Class<?> clazz = getExtensionClasses().get(key);
        if (clazz == null) {
            throw new IllegalArgumentException("非法参数:" + key);
        }
        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                //反射生成对象
                instance = (T) clazz.newInstance();
                EXTENSION_INSTANCES.putIfAbsent(clazz, instance);
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Extension instance(name: " + key + ", class: " + clazz + ")  could not be instantiated: " + e.getMessage(), e);
        }
    }


    /**
     * 获取类加载器
     */
    private static ClassLoader getClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }


    /**
     * 根据类加载器和文件名获取资源的定位符
     *
     * @param fileName    文件名
     * @param classLoader 类加载器
     */
    private static Enumeration<URL> getUrlEnumeration(String fileName, ClassLoader classLoader) {
        Enumeration<URL> urls = null;
        try {
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urls;
    }


    public ExtensionLoader(Class<T> clazz) {
        this.clazz = clazz;
    }


}
