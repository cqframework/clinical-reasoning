package org.opencds.cqf.fhir.cr.measure.common;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Architecture tests enforcing the hexagonal boundary of the measure evaluation domain.
 *
 * <p>The {@code common} package contains version-agnostic domain and evaluation logic.
 * It must never depend on FHIR-version-specific model classes or on transport-layer
 * (HTTP/REST) exception types. Version-specific code lives in the {@code r4} and
 * {@code dstu3} packages; transport exceptions are translated at the adapter boundary
 * in {@code cqf-fhir-cr-hapi}.
 */
class MeasureArchitectureTest {

    private static JavaClasses commonClasses;

    @BeforeAll
    static void importClasses() {
        commonClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("org.opencds.cqf.fhir.cr.measure.common");
    }

    @Test
    void commonMustNotDependOnR4Models() {
        noClasses()
                .that()
                .resideInAPackage("..measure.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.hl7.fhir.r4..")
                .as("common/ must not import R4 FHIR model types")
                .check(commonClasses);
    }

    @Test
    void commonMustNotDependOnDstu3Models() {
        noClasses()
                .that()
                .resideInAPackage("..measure.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.hl7.fhir.dstu3..")
                .as("common/ must not import DSTU3 FHIR model types")
                .check(commonClasses);
    }

    @Test
    void commonMustNotDependOnR5Models() {
        noClasses()
                .that()
                .resideInAPackage("..measure.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("org.hl7.fhir.r5..")
                .as("common/ must not import R5 FHIR model types")
                .check(commonClasses);
    }

    @Test
    void commonMustNotDependOnR4MeasureCode() {
        noClasses()
                .that()
                .resideInAPackage("..measure.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..measure.r4..")
                .as("common/ must not import from the r4 measure package")
                .check(commonClasses);
    }

    @Test
    void commonMustNotDependOnDstu3MeasureCode() {
        noClasses()
                .that()
                .resideInAPackage("..measure.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..measure.dstu3..")
                .as("common/ must not import from the dstu3 measure package")
                .check(commonClasses);
    }

    @Test
    void commonMustNotDependOnTransportExceptions() {
        noClasses()
                .that()
                .resideInAPackage("..measure.common..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("ca.uhn.fhir.rest.server.exceptions..")
                .as("common/ must not use HAPI REST transport exceptions — use domain exceptions instead")
                .check(commonClasses);
    }
}
