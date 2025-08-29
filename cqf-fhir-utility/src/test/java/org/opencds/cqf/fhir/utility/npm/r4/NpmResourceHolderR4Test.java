package org.opencds.cqf.fhir.utility.npm.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.npm.BaseNpmResourceInfoForCqlTest;

@SuppressWarnings("squid:S2699")
class NpmResourceHolderR4Test extends BaseNpmResourceInfoForCqlTest {

    protected FhirVersionEnum fhirVersion = FhirVersionEnum.R4;

    private static final String EXPECTED_CQL_ALPHA =
            """
        library opencds.simplealpha.SimpleAlpha

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        parameter "Measurement Period" Interval<DateTime>
          default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)

        context Patient

        define "Initial Population":
            exists ("Encounter Finished")

        define "Encounter Finished":
          [Encounter] E
            where E.status = 'finished'
        """;

    private static final String EXPECTED_CQL_BRAVO =
            """
        library opencds.simplealpha.SimpleBravo

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        parameter "Measurement Period" Interval<DateTime>
          default Interval[@2024-01-01T00:00:00.0-06:00, @2025-01-01T00:00:00.0-06:00)

        context Patient

        define "Initial Population":
            exists ("Encounter Planned")

        define "Encounter Planned":
          [Encounter] E
            where E.status = 'planned'
        """;

    private static final String EXPECTED_CQL_WITH_DERIVED =
            "library WithDerivedLibrary version '0.3'  using FHIR version '4.0.1'  include DerivedLibrary version '0.4'  parameter \"Measurement Period\" Interval<DateTime>     default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)  context Patient  define \"Initial Population\":     DerivedLibrary.\"Has Initial Population\"  define \"Denominator\":     \"Initial Population\"  define \"Numerator\":     \"Initial Population\" ";
    private static final String EXPECTED_CQL_DERIVED =
            "library DerivedLibrary version '0.4'  using FHIR version '4.0.1'  context Patient  define \"Has Initial Population\": true ";

    private static final String EXPECTED_CQL_DERIVED_TWO_LAYERS =
            "library WithTwoLayersDerivedLibraries version '0.1'  using FHIR version '4.0.1'  include DerivedLayer1a version '0.1' include DerivedLayer1b version '0.1'  parameter \"Measurement Period\" Interval<DateTime>      default Interval[@2022-01-01T00:00:00.0-06:00, @2023-01-01T00:00:00.0-06:00)  context Patient  define \"Initial Population\":     DerivedLayer1a.\"Has Initial Population\"  define \"Denominator\":     DerivedLayer1b.\"Has Denominator\"  define \"Numerator\":     DerivedLayer1b.\"Has Numerator\" ";

    private static final String EXPECTED_CQL_DERIVED_1_A =
            "library DerivedLayer1a version '0.1'  using FHIR version '4.0.1'  include DerivedLayer2a version '0.1' include DerivedLayer2b version '0.1'  define \"Has Initial Population\":     DerivedLayer2a.\"Has Initial Population\" ";

    private static final String EXPECTED_CQL_DERIVED_1_B =
            "library DerivedLayer1b version '0.1'  using FHIR version '4.0.1'  include DerivedLayer2a version '0.1' include DerivedLayer2b version '0.1'  define \"Has Denominator\":     DerivedLayer2a.\"Has Denominator\"  define \"Has Numerator\":     DerivedLayer2b.\"Has Numerator\" ";

    private static final String EXPECTED_CQL_DERIVED_2_A =
            "library DerivedLayer2a version '0.1'  using FHIR version '4.0.1'  define \"Has Initial Population\": true define \"Has Denominator\": true ";

    private static final String EXPECTED_CQL_DERIVED_2_B =
            "library DerivedLayer2b version '0.1'  using FHIR version '4.0.1'  define \"Has Numerator\": true ";

    private static final String EXPECTED_CQL_CROSS_SOURCE =
            """
        library opencds.crosspackagesource.CrossPackageSource version '0.2'

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers
        include opencds.crosspackagetarget.CrossPackageTarget version '0.3' called CrossPackageTarget

        parameter "Measurement Period" Interval<DateTime>
            default Interval[@2020-01-01T00:00:00.0-06:00, @2021-01-01T00:00:00.0-06:00)

        context Patient

        define "Initial Population":
            exists (CrossPackageTarget."Encounter Finished")
        """;

    private static final String EXPECTED_CQL_CROSS_TARGET =
            """
        library opencds.crosspackagetarget.CrossPackageTarget version '0.3'

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        context Patient

        define "Encounter Finished":
          [Encounter] E
            where E.status = 'finished'
        """;

    @Override
    protected FhirVersionEnum getExpectedFhirVersion() {
        return FhirVersionEnum.R4;
    }

    @Test
    void simpleAlpha() {
        simpleCommon(Path.of(SIMPLE_ALPHA_TGZ), MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA, EXPECTED_CQL_ALPHA);
    }

    @Test
    void simpleBravo() {
        simpleCommon(Path.of(SIMPLE_BRAVO_TGZ), MEASURE_URL_BRAVO, LIBRARY_URL_BRAVO, EXPECTED_CQL_BRAVO);
    }

    @Test
    void multiplePackages() {
        multiplePackages(EXPECTED_CQL_ALPHA, EXPECTED_CQL_BRAVO);
    }

    @Test
    void derivedLibrary() {
        derivedLibrary(EXPECTED_CQL_WITH_DERIVED, EXPECTED_CQL_DERIVED);
    }

    @Test
    void derivedLibraryTwoLayers() {
        derivedLibraryTwoLayers(
                EXPECTED_CQL_DERIVED_TWO_LAYERS,
                EXPECTED_CQL_DERIVED_1_A,
                EXPECTED_CQL_DERIVED_1_B,
                EXPECTED_CQL_DERIVED_2_A,
                EXPECTED_CQL_DERIVED_2_B);
    }

    @Test
    void crossPackage() {
        crossPackage(EXPECTED_CQL_CROSS_SOURCE, EXPECTED_CQL_CROSS_TARGET);
    }
}
