package client;

import com.common.extension.ExtensionLoader;
import com.rpccenter.proxy.ClientProxy;
import com.rpccenter.remote.Netty.Client.RpcRequestTransport;
import com.rpccenter.remote.dto.RpcServiceConfig;
import com.rpccenter.spring.serviceAnnotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import service.AddService;

import java.net.UnknownHostException;

/**
 * @author yls91
 */
@RpcScan(basePackage = {"com.rpccenter"})
public class ClientMain {
    public static void main(String[] args) throws UnknownHostException {
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ClientMain.class);

        //Spring注解申请请求
//        HelloController helloController = (HelloController) applicationContext.getBean("helloController");
//        helloController.test();
//        AddReference addReference = (AddReference) applicationContext.getBean("addReference");
//        addReference.add();



        //手动申请  RpcRequest也可以，要修改ClientProxy
        for(int i = 0; i < 10; i++) {

            RpcServiceConfig rpcService = RpcServiceConfig.builder()
                    .version("1.0")
                    .group("Three").build();
            RpcRequestTransport nettyClient = ExtensionLoader.getExtensionLoader(RpcRequestTransport.class).getExtension("client");
            AddService service = new ClientProxy(nettyClient,rpcService).getProxy(AddService.class);
            service.add(7,6);



            try{
                Thread.sleep(1000*33);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //注销此server在zk上的ip
//        CuratorFramework zk = CuratorUtil.getZkClient();
//        CuratorUtil.clearRegistry(zk,new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), NettyServer.PORT));

    }
}
