package com.clara.insurancequotes;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Application.class);

    @Test
    void moduleBoundariesAreRespected() {
        MODULES.verify();
    }
}
