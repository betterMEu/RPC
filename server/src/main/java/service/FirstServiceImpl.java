package service;

import com.rpccenter.spring.serviceAnnotation.RpcService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yls91
 */
@Slf4j
@RpcService(version = "1.0",group = "group1")
public class FirstServiceImpl implements FirstService {

    @Override
    public String sayHello(Hello hello) {
        return "第一个服务，他的描述是" + hello.getDescription();
    }
}
