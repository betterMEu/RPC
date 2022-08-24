package main;


import com.rpccenter.remote.Netty.Server.NettyServer;
import com.rpccenter.spring.serviceAnnotation.RpcScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author yls91
 */
@RpcScan(basePackage = {"com.rpccenter"})
 public class ServerMain {
    public static void main(String[] args) {
        //已将NettyServer注册进spring  通过注释注册服务
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(ServerMain.class);
        NettyServer nettyServer = (NettyServer) applicationContext.getBean("nettyServer");


        //手动注册
//        FirstServiceImpl2 firstServiceImpl2 = new FirstServiceImpl2();
//        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
//                        .service(firstServiceImpl2)
//                        .version("1.0")
//                        .group("first").build();


        nettyServer.start();



    }
}
