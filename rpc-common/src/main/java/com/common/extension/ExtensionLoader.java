package com.common.extension;

import com.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author yls91
 */
@Slf4j
public class ExtensionLoader<T> {

    /**
     * 扩展类的路径
     * */
    private static final String EXTENSIONS_DIR = "extensions/";

    /**
     * 指定类的加载器
     * */
    private static final Map<Class<?>,ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    /**
     * @Key 加载完毕的类
     * @value 类的实例
     * 指定类的实例  Object类接受所有对象
     * */
    private static final Map<Class<?>,Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    /**
     * 实例缓存
     */
    private static final Map<String,Holder<Object>> cachedInstances = new ConcurrentHashMap<>();
    /**
     * 加载后的类
     * */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<>();

    private final Class<?> type;


    /**
    *   传入功能接口类
     * */
    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }



    /**
     * 获取指定类的加载器 先从SPECIFIED_CLASS_LOADERS中获取  没有则创建
     * */
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("扩展类型不可为空");
        }
        if(!type.isInterface()) {
            log.info(type.getName());
            throw new IllegalArgumentException("扩展必须实现接口");
        }
        if (type.getAnnotation(SPI.class) == null) {
            log.info(type.getName());
            throw new IllegalArgumentException("扩展必须被@SPI注解");
        }

        ExtensionLoader<T> extensionLoader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if(extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type,new ExtensionLoader<T>(type));
            extensionLoader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }


    /**
     * @param name 具体实现类
     * */
    public T getExtension(String name) {
        if(StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("扩展名不可为空");
        }

        Holder<Object> holder = cachedInstances.get(name);
        if(holder == null) {
            cachedInstances.putIfAbsent(name,new Holder<>());
            holder = cachedInstances.get(name);
        }

        Object instance = holder.get();
        if(instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if(instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }

        return (T) instance;
    }

    /**
     * 此方法下的子方法  完成了类的加载
     * 此方法完成 类的实例
     * */
    private T createExtension(String name) {
        // getExtensionClasses()返回cachedClasses，也就是key为类的别名，value为加载后的类  根据别名获取类
        Class<?> clazz = getExtensionClasses().get(name);

         //文件中不存在名称为name的扩展类才会返回null
        if(clazz == null) {
            throw new RuntimeException("没有这个扩展类" + name);
        }

        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if(instance == null) {
            try{
                EXTENSION_INSTANCES.putIfAbsent(clazz,clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return instance;
    }

    private Map<String,Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.get();

        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    loadClass(classes);
                    cachedClasses.set(classes);
                }
            }
        }
        return classes;
    }

    private void loadClass(Map<String, Class<?>> classes) {
        String fileName = ExtensionLoader.EXTENSIONS_DIR + type.getName();
        try {
            //获取 类加载器  用于加载实现了接口的类
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            Enumeration<URL> urls = classLoader.getResources(fileName);
            if (urls != null) {
                URL resourceUrl = urls.nextElement();
                loadResource(classes, classLoader, resourceUrl);
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    private void loadResource(Map<String, Class<?>> classes, ClassLoader classLoader, URL classUrl) {
        //进入了文件
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(classUrl.openStream(),"UTF-8"))) {
            String line;

            // 一行一行读取文件内容
            while ((line = reader.readLine()) != null) {

                // line的形式：kyro=github.javaguide.serialize.kyro.KryoSerializer  #这是kryo序列化的实现类
                final int ci = line.indexOf('#');

                //没有#字符返回 -1
                if (ci >= 0) {
                    line = line.substring(0, ci);
                }

                //删除首尾的空格
                line = line.trim();

                if (line.length() > 0) {
                    try {
                        // line的形式：kyro=github.javaguide.serialize.kyro.KryoSerializer
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();

                        if (name.length() > 0 && clazzName.length() > 0) {
                            /**
                             * 真正加载类的时刻
                             * */
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            classes.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


}
