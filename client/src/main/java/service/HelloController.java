package service;

import com.rpccenter.spring.serviceAnnotation.RpcReference;
import org.springframework.stereotype.Component;

/**
 * @author yls91
 */
@Component
public class HelloController {

    @RpcReference(group = "group1",version = "1.0")
    private FirstService firstService;

    public void test() {
        firstService.sayHello(new Hello("信息", "描述"));
    }

}