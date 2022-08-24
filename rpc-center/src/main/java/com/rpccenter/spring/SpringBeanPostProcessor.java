package com.rpccenter.spring;

import com.common.extension.ExtensionLoader;
import com.common.factory.SingletonFactory;
import com.rpccenter.proxy.ClientProxy;
import com.rpccenter.remote.ServiceProvider.Impl.ServiceProviderImpl;
import com.rpccenter.remote.ServiceProvider.ServiceProvider;
import com.rpccenter.remote.Netty.Client.RpcRequestTransport;
import com.rpccenter.remote.dto.RpcServiceConfig;
import com.rpccenter.spring.serviceAnnotation.RpcReference;
import com.rpccenter.spring.serviceAnnotation.RpcService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * @author yls91
 */
@Component
@Slf4j
public class SpringBeanPostProcessor implements BeanPostProcessor {
    private final ServiceProvider serviceProvider;
    private final RpcRequestTransport rpcClient;

    public SpringBeanPostProcessor() {
        serviceProvider = SingletonFactory.getInstance(ServiceProviderImpl.class);
        rpcClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension("client");
    }

    @SneakyThrows
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        boolean flag = bean.getClass().isAnnotationPresent(RpcService.class);
        if(flag) {
            log.info("[{}] 被注解了[{}]", bean.getClass().getName(), RpcService.class.getName());

            //bean :FirstServiceImpl
            RpcService Rpcservice = bean.getClass().getAnnotation(RpcService.class);
            RpcServiceConfig rpcService = RpcServiceConfig.builder()
                    .version(Rpcservice.version())
                    .group(Rpcservice.group())
                    .service(bean)
                    .build();

            serviceProvider.publishService(rpcService);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> targetClass = bean.getClass();
        Field[] declaredFields = targetClass.getDeclaredFields();

        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                RpcServiceConfig rpcService = RpcServiceConfig.builder()
                        .group(rpcReference.group())
                        .version(rpcReference.version())
                        .build();

                ClientProxy clientProxy = new ClientProxy(rpcClient, rpcService);

                //declaredField.getType()获取字段类型  getProxy(传入功能接口)
                Object proxy = clientProxy.getProxy(declaredField.getType());
                declaredField.setAccessible(true);
                try {
                    //bean的declaredField字段的值修改成proxy
                    declaredField.set(bean, proxy);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }

}
