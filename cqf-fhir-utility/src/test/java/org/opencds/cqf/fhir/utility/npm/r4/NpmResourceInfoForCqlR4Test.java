package org.opencds.cqf.fhir.utility.npm.r4;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.utility.npm.BaseNpmResourceInfoForCqlTest;

@SuppressWarnings("squid:S2699")
class NpmResourceInfoForCqlR4Test extends BaseNpmResourceInfoForCqlTest {

    protected FhirVersionEnum fhirVersion = FhirVersionEnum.R4;

    private static final String EXPECTED_CQL_ALPHA =
            "library SimpleAlpha  parameter \"Measurement Period\" Interval<DateTime> default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)  define \"Initial Population\": true ";
    private static final String EXPECTED_CQL_BRAVO =
            "library SimpleBravo  parameter \"Measurement Period\" Interval<DateTime>   default Interval[@2024-01-01T00:00:00.0-06:00, @2025-01-01T00:00:00.0-06:00)  define \"Initial Population\": true ";
    private static final String EXPECTED_CQL_WITH_DERIVED =
            "library WithDerivedLibrary version '0.1'  using FHIR version '4.0.1'  include DerivedLibrary version '0.1'  parameter \"Measurement Period\" Interval<DateTime>     default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)  context Patient  define \"Initial Population\":     DerivedLibrary.\"Has Initial Population\"  define \"Denominator\":     \"Initial Population\"  define \"Numerator\":     \"Initial Population\" ";
    private static final String EXPECTED_CQL_DERIVED =
            "library DerivedLibrary version '0.1'  define \"Has Initial Population\": true ";

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
            "library CrossPackageSource version '0.1'  using FHIR version '4.0.1'  include opencds.crosspackagetarget.CrossPackageTarget version '0.2' called CrossPackageTarget  parameter \"Measurement Period\" Interval<DateTime>     default Interval[@2020-01-01T00:00:00.0-06:00, @2021-01-01T00:00:00.0-06:00)  context Patient  define \"Initial Population\":     CrossPackageTarget.\"Has Initial Population\"  define \"Denominator\":     CrossPackageTarget.\"Has Denominator\"  define \"Numerator\":     CrossPackageTarget.\"Has Numerator\" ";

    private static final String EXPECTED_CQL_CROSS_TARGET =
            "library CrossPackageTarget version '0.2'  define \"Has Initial Population\": true  define \"Has Denominator\": true  define \"Has Numerator\": true ";

    @Override
    protected FhirVersionEnum getExpectedFhirVersion() {
        return FhirVersionEnum.R4;
    }

    private static Stream<Arguments> simplePackagesParams() {
        return Stream.of(
                Arguments.of(SIMPLE_ALPHA_TGZ, MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA, EXPECTED_CQL_ALPHA),
                Arguments.of(SIMPLE_BRAVO_TGZ, MEASURE_URL_BRAVO, LIBRARY_URL_BRAVO, EXPECTED_CQL_BRAVO));
    }

    @ParameterizedTest
    @MethodSource("simplePackagesParams")
    void simple(Path tgzPath, String measureUrl, String expectedLibraryUrl, String expectedCql) {
        simpleCommon(tgzPath, measureUrl, expectedLibraryUrl, expectedCql);
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
