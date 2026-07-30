package com.clara.insurancequotes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private static final ApplicationModules MODULES = ApplicationModules.of(Application.class);
    private static final JavaClasses APPLICATION_CLASSES =
            new ClassFileImporter().importPath(Path.of("target", "classes"));

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
        assertThat(quote.getNamedInterfaces().getByName("quote-api-exception")).isPresent();
        assertThat(quote.getNamedInterfaces().getByName("quote-domain-model")).isEmpty();
    }

    @Test
    void exposesAuthenticationUseCasesAsThePublicAuthContract() {
        var auth = MODULES.getModuleByName("auth").orElseThrow();

        assertThat(auth.getNamedInterfaces().getByName("auth-api-usecase")).isPresent();
        assertThat(auth.getNamedInterfaces().getByName("auth-api-result")).isPresent();
        assertThat(auth.getNamedInterfaces().getByName("auth-api-exception")).isPresent();
        assertThat(auth.getNamedInterfaces().getByName("auth-adapter-in-web-request"))
                .isEmpty();
    }

    @Test
    void placesSharedCrossCuttingComponentsInResponsibilityPackages() throws Exception {
        var shared = MODULES.getModuleByName("shared").orElseThrow();

        assertThat(shared.getNamedInterfaces().getByName("shared-observability"))
                .isPresent();
        assertThat(Class.forName("com.clara.insurancequotes.shared.observability.BusinessMetrics"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.shared.adapter.in.web.filter.CorrelationIdFilter"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.shared.configuration.OpenApiConfiguration"))
                .isNotNull();
        assertThat(Class.forName("com.clara.insurancequotes.shared.configuration.WebVersioningConfiguration"))
                .isNotNull();

        assertLegacyConfigClassIsAbsent("BusinessMetrics");
        assertLegacyConfigClassIsAbsent("CorrelationIdFilter");
        assertLegacyConfigClassIsAbsent("OpenApiConfig");
        assertLegacyConfigClassIsAbsent("WebVersioningConfig");
    }

    @Test
    void corePackagesDoNotDependOnOutboundOrInboundAdapterPackages() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..api.command..",
                        "..api.query..",
                        "..api.result..",
                        "..api.event..",
                        "..api.usecase..",
                        "..api.type..",
                        "..api.exception..",
                        "..application..",
                        "..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..adapter..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void corePackagesDoNotDependOnTransportOrInfrastructureTypes() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..api.command..",
                        "..api.query..",
                        "..api.usecase..",
                        "..api.type..",
                        "..api.exception..",
                        "..application..",
                        "..domain..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "org.springframework.http..",
                        "org.springframework.kafka..",
                        "org.springframework.data.redis..",
                        "com.fasterxml.jackson..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void transportRequestsRemainInboundWebAdapterDetails() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Request")
                .should()
                .resideInAnyPackage("..adapter.in.web.request..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void persistenceAdaptersAreNamedByTheirApplicationCapability() {
        classes()
                .that()
                .haveSimpleNameEndingWith("PersistenceAdapter")
                .should()
                .resideInAnyPackage("..adapter.out.persistence..")
                .check(APPLICATION_CLASSES);
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
