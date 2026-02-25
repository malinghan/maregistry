package com.malinghan.maregistry;

import com.malinghan.maregistry.model.InstanceMeta;
import com.malinghan.maregistry.service.MaRegistryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MaRegistryV1Test {

    MaRegistryService service;

    @BeforeEach
    void setUp() {
        service = new MaRegistryService();
    }

    @Test
    void registerAndFindAll() {
        InstanceMeta inst = InstanceMeta.http("localhost", 8080);
        service.register("com.malinghan.UserService", inst);

        List<InstanceMeta> result = service.getAllInstances("com.malinghan.UserService");
        System.out.println("Registered instances: " + result);
        assertEquals(1, result.size());
        assertEquals("localhost", result.get(0).getHost());
    }

    @Test
    void noDuplicateRegister() {
        InstanceMeta inst = InstanceMeta.http("localhost", 8080);
        service.register("com.malinghan.UserService", inst);
        service.register("com.malinghan.UserService", inst);

        List<InstanceMeta> result = service.getAllInstances("com.malinghan.UserService");
        System.out.println("After duplicate register: " + result);
        assertEquals(1, result.size());
    }

    @Test
    void unregister() {
        InstanceMeta inst1 = InstanceMeta.http("localhost", 8080);
        InstanceMeta inst2 = InstanceMeta.http("localhost", 8081);
        service.register("com.malinghan.UserService", inst1);
        service.register("com.malinghan.UserService", inst2);
        service.unregister("com.malinghan.UserService", inst1);

        List<InstanceMeta> result = service.getAllInstances("com.malinghan.UserService");
        System.out.println("After unregister inst1: " + result);
        assertEquals(1, result.size());
        assertEquals(8081, result.get(0).getPort());
    }

    @Test
    void multipleServices() {
        service.register("com.malinghan.UserService", InstanceMeta.http("host1", 8080));
        service.register("com.malinghan.OrderService", InstanceMeta.http("host2", 9090));

        List<InstanceMeta> users = service.getAllInstances("com.malinghan.UserService");
        List<InstanceMeta> orders = service.getAllInstances("com.malinghan.OrderService");
        System.out.println("UserService instances: " + users);
        System.out.println("OrderService instances: " + orders);
        assertEquals(1, users.size());
        assertEquals(1, orders.size());
    }
}