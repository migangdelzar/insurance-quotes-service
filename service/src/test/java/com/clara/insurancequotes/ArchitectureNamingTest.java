package com.clara.insurancequotes;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;

class ArchitectureNamingTest {

    private static final JavaClasses APPLICATION_CLASSES =
            new ClassFileImporter().importPath(Path.of("target", "classes"));

    @Test
    void springConfigurationClassesUseConfigurationSuffix() {
        classes()
                .that()
                .areAnnotatedWith(Configuration.class)
                .should()
                .haveSimpleNameEndingWith("Configuration")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void useCasesArePublicContracts() {
        classes()
                .that()
                .haveSimpleNameEndingWith("UseCase")
                .should()
                .resideInAnyPackage("..api.usecase..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void requestsRemainInboundWebAdapterDetails() {
        classes()
                .that()
                .haveSimpleNameEndingWith("Request")
                .should()
                .resideInAnyPackage("..adapter.in.web.request..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void persistenceAdaptersRemainOutboundPersistenceDetails() {
        classes()
                .that()
                .haveSimpleNameEndingWith("PersistenceAdapter")
                .should()
                .resideInAnyPackage("..adapter.out.persistence..")
                .check(APPLICATION_CLASSES);
    }

    @Test
    void corePackagesDoNotDependOnWebOrInfrastructureTypes() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..api.command..",
                        "..api.query..",
                        "..api.result..",
                        "..api.type..",
                        "..api.usecase..",
                        "..api.exception..",
                        "..application..",
                        "..domain..")
                .and()
                .doNotHaveFullyQualifiedName(
                        "com.clara.insurancequotes.auth.api.result.LoginResponse")
                .and()
                .doNotHaveFullyQualifiedName(
                        "com.clara.insurancequotes.auth.api.result.TokenPairResponse")
                .and()
                .doNotHaveFullyQualifiedName(
                        "com.clara.insurancequotes.auth.api.result.WebAuthnChallengeResponse")
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
}
