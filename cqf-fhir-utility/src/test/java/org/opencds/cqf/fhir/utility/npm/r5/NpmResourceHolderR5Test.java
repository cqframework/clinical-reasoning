package org.opencds.cqf.fhir.utility.npm.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.npm.BaseNpmResourceInfoForCqlTest;

@SuppressWarnings("squid:S2699")
class NpmResourceHolderR5Test extends BaseNpmResourceInfoForCqlTest {

    protected FhirVersionEnum fhirVersion = FhirVersionEnum.R5;

    private static final String EXPECTED_CQL_ALPHA = """
        """;
    private static final String EXPECTED_CQL_BRAVO = """
        """;
    private static final String EXPECTED_CQL_WITH_DERIVED = """
        """;
    private static final String EXPECTED_CQL_DERIVED = """
        """;

    private static final String EXPECTED_CQL_DERIVED_TWO_LAYERS =
            """
        library opencds.withtwolayersderivedlibraries.WithTwoLayersDerivedLibraries version '0.5'

        using FHIR version '5.0.1'

        include DerivedLayer1a version '0.5'
        include DerivedLayer1b version '0.5'

        parameter "Measurement Period" Interval<DateTime>
            default Interval[@2022-01-01T00:00:00.0-06:00, @2023-01-01T00:00:00.0-06:00)

        context Patient

        define "Initial Population":
            DerivedLayer1a."Initial Population"

        define "Denominator":
            DerivedLayer1b."Denominator"

        define "Numerator":
            DerivedLayer1b."Numerator"
        """;

    private static final String EXPECTED_CQL_DERIVED_1_A =
            """
        library opencds.withtwolayersderivedlibraries.DerivedLayer1a version '0.5'

        using FHIR version '5.0.1'

        include DerivedLayer2a version '0.5'
        include DerivedLayer2b version '0.5'

        context Patient

        define "Initial Population":
            DerivedLayer2a."Initial Population"
        """;

    private static final String EXPECTED_CQL_DERIVED_1_B =
            """
        library opencds.withtwolayersderivedlibraries.DerivedLayer1b version '0.5'

        using FHIR version '5.0.1'

        include DerivedLayer2a version '0.5'
        include DerivedLayer2b version '0.5'

        context Patient

        define "Denominator":
            DerivedLayer2a."Denominator"

        define "Numerator":
            DerivedLayer2b."Numerator"
        """;

    private static final String EXPECTED_CQL_DERIVED_2_A =
            """
        library opencds.withtwolayersderivedlibraries.DerivedLayer2a version '0.5'

        using FHIR version '5.0.1'

        include FHIRHelpers version '5.0.1' called FHIRHelpers

        context Patient

        define "Initial Population":
            exists ("Encounter Finished")

        define "Denominator":
            exists ("Encounter Planned")

        define "Encounter Finished":
          [Encounter] E
            where E.status = 'finished'

        define "Encounter Planned":
          [Encounter] E
            where E.status = 'planned'
        """;

    private static final String EXPECTED_CQL_DERIVED_2_B =
            """
        library opencds.withtwolayersderivedlibraries.DerivedLayer2b version '0.5'

        using FHIR version '5.0.1'

        include FHIRHelpers version '5.0.1' called FHIRHelpers

        parameter "Measurement Period" Interval<DateTime>
          default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)

        context Patient

        define "Numerator":
            exists ("Encounter Triaged")

        define "Encounter Triaged":
          [Encounter] E
            where E.status = 'triaged'
        """;

    private static final String EXPECTED_CQL_CROSS_SOURCE = """
        """;

    private static final String EXPECTED_CQL_CROSS_TARGET = """
        """;

    @Override
    protected FhirVersionEnum getExpectedFhirVersion() {
        return FhirVersionEnum.R5;
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
