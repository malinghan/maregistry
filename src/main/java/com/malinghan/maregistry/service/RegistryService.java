package com.malinghan.maregistry.service;

import com.malinghan.maregistry.model.InstanceMeta;

import java.util.List;

public interface RegistryService {

    InstanceMeta register(String service, InstanceMeta instance);

    InstanceMeta unregister(String service, InstanceMeta instance);

    List<InstanceMeta> getAllInstances(String service);
}