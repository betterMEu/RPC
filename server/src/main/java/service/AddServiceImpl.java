package service;

import com.rpccenter.spring.serviceAnnotation.RpcService;

/**
 * @author yls91
 */
@RpcService(version = "1.0",group = "Three")
public class AddServiceImpl implements AddService {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
