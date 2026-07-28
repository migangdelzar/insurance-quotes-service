package com.clara.insurancequotes;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Application.class);

    @Test
    void moduleBoundariesAreRespected() {
        MODULES.verify();
    }

    @Test
    void generateModuleDocumentation() {
        new org.springframework.modulith.docs.Documenter(
                        MODULES,
                        Path.of("..", "docs", "architecture", "modules").toString())
                .writeDocumentation();
    }
}
