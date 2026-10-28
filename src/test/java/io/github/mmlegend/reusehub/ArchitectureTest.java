package io.github.mmlegend.reusehub;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

class ArchitectureTest {
    private final com.tngtech.archunit.core.domain.JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.mmlegend.reusehub");

    @Test void controllersDoNotAccessRepositories() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("io.github.mmlegend.reusehub..persistence..").check(classes);
    }

    @Test void domainDoesNotDependOnApplicationOrTransport() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..application..", "..api..", "io.github.mmlegend.reusehub..persistence..",
                        "org.springframework..", "jakarta.servlet..").check(classes);
    }

    @Test void featureModulesHaveNoCycles() {
        slices().matching("io.github.mmlegend.reusehub.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test void identityInternalsArePrivateToTheModule() {
        noClasses().that().resideOutsideOfPackage("..identity..")
                .should().dependOnClassesThat().resideInAnyPackage("..identity.domain..", "..identity.persistence..")
                .check(classes);
    }

    @Test void offersInternalsArePrivateToTheModule() {
        noClasses().that().resideOutsideOfPackage("..offers..")
                .should().dependOnClassesThat().resideInAnyPackage("..offers.domain..", "..offers.persistence..")
                .check(classes);
    }
}
