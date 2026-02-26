package com.malinghan.maregistry.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.malinghan.maregistry.cluster.MaRegistryConfigProperties;
import com.malinghan.maregistry.cluster.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileRegistryStore implements RegistryStore {

    private static final Logger log = LoggerFactory.getLogger(FileRegistryStore.class);

    private final ObjectMapper objectMapper;
    private final String snapshotPath;

    public FileRegistryStore(ObjectMapper objectMapper, MaRegistryConfigProperties properties) {
        this.objectMapper = objectMapper;
        this.snapshotPath = properties.getSnapshotPath();
    }

    @Override
    public void save(Snapshot snapshot) {
        try {
            Path target = Paths.get(snapshotPath);
            Files.createDirectories(target.getParent());
            Path tmp = Paths.get(snapshotPath + ".tmp");
            objectMapper.writeValue(tmp.toFile(), snapshot);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Failed to save snapshot to {}", snapshotPath, e);
        }
    }

    @Override
    public Snapshot load() {
        File file = new File(snapshotPath);
        if (!file.exists()) {
            return null;
        }
        try {
            return objectMapper.readValue(file, Snapshot.class);
        } catch (IOException e) {
            log.error("Failed to load snapshot from {}", snapshotPath, e);
            return null;
        }
    }
}
