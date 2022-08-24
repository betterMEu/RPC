package com.rpccenter.remote.codec.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.rpccenter.remote.codec.serializer.Serializer;
import com.rpccenter.remote.dto.RpcRequest;
import com.rpccenter.remote.dto.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


/**
 * @author yls91
 */
@Slf4j
public class KryoSerializer implements Serializer {

    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(()-> {
        Kryo kryo = new Kryo();
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        return kryo;
    });

    @Override
    public byte[] serialize(Object object) {
        try (Output output = new Output(new ByteArrayOutputStream())) {
            Kryo kryo = kryoThreadLocal.get();
            // object to bytes
            //用注册的序列化方式 序列化obj
            kryo.writeObject(output,object);
            kryoThreadLocal.remove();
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("Serialization is failed");
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try(Input input = new Input(new ByteArrayInputStream(bytes))) {
            Kryo kryo = kryoThreadLocal.get();
            Object o = kryo.readObject(input,clazz);
            kryoThreadLocal.remove();
            return clazz.cast(o);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization is failed");
        }
    }
}