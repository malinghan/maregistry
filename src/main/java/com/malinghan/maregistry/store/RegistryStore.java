package com.malinghan.maregistry.store;

import com.malinghan.maregistry.cluster.Snapshot;

public interface RegistryStore {
    void save(Snapshot snapshot);
    Snapshot load();
}
