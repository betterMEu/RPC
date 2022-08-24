package service;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yls91
 */
@Slf4j
public class FirstServiceImpl2 implements FirstService {


    @Override
    public String sayHello(Hello hello) {
        return "第一个服务的第二种实现" + hello.getDescription();
    }
}