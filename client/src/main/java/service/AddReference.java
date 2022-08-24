package service;

import com.rpccenter.spring.serviceAnnotation.RpcReference;
import org.springframework.stereotype.Component;

/**
 * @author yls91
 */
@Component
public class AddReference {

    @RpcReference(version = "1.0",group = "Three")
    private AddService addService;

    public void add() {
        addService.add(5,6);
    }
}
