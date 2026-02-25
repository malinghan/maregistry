package com.malinghan.maregistry;

import com.malinghan.maregistry.model.InstanceMeta;
import com.malinghan.maregistry.service.RegistryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MaRegistryController {

    private final RegistryService registryService;

    public MaRegistryController(RegistryService registryService) {
        this.registryService = registryService;
    }

    @PostMapping("/reg")
    public InstanceMeta register(@RequestParam String service, @RequestBody InstanceMeta instance) {
        return registryService.register(service, instance);
    }

    @PostMapping("/unreg")
    public InstanceMeta unregister(@RequestParam String service, @RequestBody InstanceMeta instance) {
        return registryService.unregister(service, instance);
    }

    @GetMapping("/findAll")
    public List<InstanceMeta> findAll(@RequestParam String service) {
        return registryService.getAllInstances(service);
    }
}