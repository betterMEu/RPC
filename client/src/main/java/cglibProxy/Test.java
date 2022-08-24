package cglibProxy;

public class Test {
    public static void main(String[] args) {
        SendMessage sendMessage = (SendMessage) ProxyFactory.getProxy(SendMessage.class);
        sendMessage.send("小飞棍来咯");
        sendMessage.angry();
    }
}
