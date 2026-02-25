package com.malinghan.marpc.maregistry.service;

import com.malinghan.marpc.maregistry.model.InstanceMeta;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

@Service
public class MaRegistryService implements RegistryService {

    private final MultiValueMap<String, InstanceMeta> REGISTRY = new LinkedMultiValueMap<>();

    @Override
    public synchronized InstanceMeta register(String service, InstanceMeta instance) {
        List<InstanceMeta> instances = REGISTRY.get(service);
        if (instances != null && instances.contains(instance)) {
            return instance;
        }
        REGISTRY.add(service, instance);
        return instance;
    }

    @Override
    public synchronized InstanceMeta unregister(String service, InstanceMeta instance) {
        List<InstanceMeta> instances = REGISTRY.get(service);
        if (instances != null) {
            instances.remove(instance);
        }
        return instance;
    }

    @Override
    public List<InstanceMeta> getAllInstances(String service) {
        return REGISTRY.get(service);
    }
}