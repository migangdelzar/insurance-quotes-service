package com.clara.insurancequotes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Application.class);

    @Test
    void moduleBoundariesAreRespected() {
        MODULES.verify();
    }

    @Test
    void discoversDocumentedBusinessModules() {
        assertModule("auth", "Authentication");
        assertModule("pricing", "Pricing");
        assertModule("quote", "Quote");
        assertModule("submission", "Submission");
        assertModule("shared", "Shared");
    }

    @Test
    void exposesOnlyExistingQuoteApiResponsibilitiesAsNamedInterfaces() {
        var quote = MODULES.getModuleByName("quote").orElseThrow();

        assertThat(quote.getNamedInterfaces().getByName("quote-api-command")).isPresent();
        assertThat(quote.getNamedInterfaces().getByName("quote-api-query")).isPresent();
        assertThat(quote.getNamedInterfaces().getByName("quote-api-result")).isPresent();
        assertThat(quote.getNamedInterfaces().getByName("quote-api-usecase")).isPresent();
        assertThat(quote.getNamedInterfaces().getByName("quote-api-type")).isPresent();
    }

    @Test
    void placesSharedCrossCuttingComponentsInResponsibilityPackages() throws Exception {
        var shared = MODULES.getModuleByName("shared").orElseThrow();

        assertThat(shared.getNamedInterfaces().getByName("shared-observability")).isPresent();
        assertThat(Class.forName("com.clara.insurancequotes.shared.observability.BusinessMetrics"))
                .isNotNull();
        assertThat(Class.forName(
                        "com.clara.insurancequotes.shared.adapter.in.web.filter.CorrelationIdFilter"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.shared.configuration.OpenApiConfiguration"))
                .isNotNull();
        assertThat(Class.forName(
                        "com.clara.insurancequotes.shared.configuration.WebVersioningConfiguration"))
                .isNotNull();

        assertLegacyConfigClassIsAbsent("BusinessMetrics");
        assertLegacyConfigClassIsAbsent("CorrelationIdFilter");
        assertLegacyConfigClassIsAbsent("OpenApiConfig");
        assertLegacyConfigClassIsAbsent("WebVersioningConfig");
    }

    @Test
    void generateModuleDocumentation() {
        new org.springframework.modulith.docs.Documenter(
                        MODULES,
                        Path.of("..", "docs", "architecture", "modules").toString())
                .writeDocumentation();
    }

    private void assertModule(String name, String displayName) {
        ApplicationModule module = MODULES.getModuleByName(name).orElseThrow();

        assertThat(module.getDisplayName()).isEqualTo(displayName);
    }

    private static void assertLegacyConfigClassIsAbsent(String simpleName) {
        assertThatThrownBy(() -> Class.forName("com.clara.insurancequotes.config." + simpleName))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
