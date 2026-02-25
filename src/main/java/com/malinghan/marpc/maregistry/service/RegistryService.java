package com.malinghan.marpc.maregistry.service;

import com.malinghan.marpc.maregistry.model.InstanceMeta;

import java.util.List;

public interface RegistryService {

    InstanceMeta register(String service, InstanceMeta instance);

    InstanceMeta unregister(String service, InstanceMeta instance);

    List<InstanceMeta> getAllInstances(String service);
}