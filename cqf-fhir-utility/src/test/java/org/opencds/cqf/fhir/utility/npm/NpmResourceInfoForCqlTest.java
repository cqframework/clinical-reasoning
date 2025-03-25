package org.opencds.cqf.fhir.utility.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.IMeasureAdapter;

class NpmResourceInfoForCqlTest {
    private static final String DOT_TGZ = ".tgz";

    private static final String EXPECTED_CQL_ALPHA =
            "library SimpleAlpha  parameter \"Measurement Period\" Interval<DateTime> default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)  define \"Initial Population\": true ";
    private static final String EXPECTED_CQL_BRAVO =
            "library SimpleBravo  parameter \"Measurement Period\" Interval<DateTime>   default Interval[@2024-01-01T00:00:00.0-06:00, @2025-01-01T00:00:00.0-06:00)  define \"Initial Population\": true ";
    private static final String EXPECTED_CQL_WITH_DERIVED =
            "library WithDerivedLibrary version '0.1'  using FHIR version '4.0.1'  include DerivedLibrary version '0.1'  parameter \"Measurement Period\" Interval<DateTime>     default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)  context Patient  define \"Initial Population\":     DerivedLibrary.\"Has Initial Population\"  define \"Denominator\":     \"Initial Population\"  define \"Numerator\":     \"Initial Population\" ";
    private static final String EXPECTED_CQL_DERIVED =
            "library DerivedLibrary version '0.1'  define \"Has Initial Population\": true ";

    private static final String EXPECTED_CQL_DERIVED_TWO_LAYERS =
            "library with-two-layers-derived-libraries '0.1'  using FHIR version '4.0.1'  include derived-layer-1a version '0.1.a' include derived-layer-1b version '0.1.b'  parameter \"Measurement Period\" Interval<DateTime>     default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)   context Patient  define \"Initial Population\":     derived-layer-1a.\"Has Initial Population\"  define \"Denominator\":     derived-layer-1a.\"Has Denominator\"  define \"Numerator\":     derived-layer-1b.\"Has Numerator\" ";

    private static final String EXPECTED_CQL_DERIVED_1_A =
            "library derived-layer-1a version '0.1.a'  include derived-layer-2a version '0.2.a' include derived-layer-2b version '0.2.b'  define \"Has Initial Population\":     derived-layer-2a.\"Has Initial Population\"  define \"Has Denominator\":     derived-layer-2b.\"Has Denominator\" ";

    private static final String EXPECTED_CQL_DERIVED_1_B =
            "library derived-layer-1b version '0.1.b'  include derived-layer-2a version '0.2.a' include derived-layer-2b version '0.2.b'  define \"Has Numerator\":     derived-layer-2a.\"Has Numerator\" ";

    private static final String EXPECTED_CQL_DERIVED_2_A =
            "library derived-layer-2a version '0.2.a'  define \"Has Initial Population\": true ";

    private static final String EXPECTED_CQL_DERIVED_2_B =
            "library derived-layer-2b version '0.2.b'  define \"Has Numerator\": true ";

    private static final String SIMPLE_ALPHA = "simple-alpha";
    private static final String SIMPLE_BRAVO = "simple-bravo";
    private static final String WITH_DERIVED_LIBRARY = "with-derived-library";
    private static final String WITH_DERIVED_LIBRARY_UPPER = "WithDerivedLibrary";
    private static final String DERIVED_LIBRARY_ID = "DerivedLibrary";
    private static final String DERIVED_LIBRARY = DERIVED_LIBRARY_ID;

    private static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES = "with-two-layers-derived-libraries";

    private static final String SIMPLE_ALPHA_TGZ = SIMPLE_ALPHA + DOT_TGZ;
    private static final String SIMPLE_BRAVO_TGZ = SIMPLE_BRAVO + DOT_TGZ;
    private static final Path WITH_DERIVED_LIBRARY_TGZ = Paths.get(WITH_DERIVED_LIBRARY + DOT_TGZ);
    private static final Path WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ =
            Paths.get(WITH_TWO_LAYERS_DERIVED_LIBRARIES + DOT_TGZ);

    private static final String SLASH_MEASURE_SLASH = "/Measure/";
    private static final String SLASH_LIBRARY_SLASH = "/Library/";

    private static final String SIMPLE_URL = "http://example.com";
    private static final String DERIVED_URL = "http://with-derived-library.npm.opencds.org";
    private static final String DERIVED_TWO_LAYERS_URL = "http://with-two-layers-derived-libraries.npm.opencds.org";

    private static final String MEASURE_URL_ALPHA = SIMPLE_URL + SLASH_MEASURE_SLASH + SIMPLE_ALPHA;
    private static final String MEASURE_URL_BRAVO = SIMPLE_URL + SLASH_MEASURE_SLASH + SIMPLE_BRAVO;
    private static final String MEASURE_URL_WITH_DERIVED_LIBRARY =
            DERIVED_URL + SLASH_MEASURE_SLASH + WITH_DERIVED_LIBRARY_UPPER;
    private static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES =
            DERIVED_TWO_LAYERS_URL + SLASH_MEASURE_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES;

    private static final String LIBRARY_URL_ALPHA = SIMPLE_URL + SLASH_LIBRARY_SLASH + SIMPLE_ALPHA;
    private static final String LIBRARY_URL_BRAVO = SIMPLE_URL + SLASH_LIBRARY_SLASH + SIMPLE_BRAVO;
    private static final String LIBRARY_URL_WITH_DERIVED_LIBRARY =
            DERIVED_URL + SLASH_LIBRARY_SLASH + WITH_DERIVED_LIBRARY_UPPER;
    private static final String LIBRARY_URL_DERIVED_LIBRARY = DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LIBRARY;

    private static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES =
            DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES;

    private static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A =
            DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + "derived-layer-1a";
    private static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B =
            DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + "derived-layer-1b";
    private static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A =
            DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + "derived-layer-2a";
    private static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B =
            DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + "derived-layer-2b";

    private static Stream<Arguments> simplePackagesParams() {
        return Stream.of(
                Arguments.of(SIMPLE_ALPHA_TGZ, MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA, EXPECTED_CQL_ALPHA),
                Arguments.of(SIMPLE_BRAVO_TGZ, MEASURE_URL_BRAVO, LIBRARY_URL_BRAVO, EXPECTED_CQL_BRAVO));
    }

    @ParameterizedTest
    @MethodSource("simplePackagesParams")
    void simple(Path tgzPath, String measureUrl, String expectedLibraryUrl, String expectedCql) {
        final NpmPackageLoaderInMemory loader = setup(tgzPath);

        final NpmResourceInfoForCql npmResourceInfoForCql = loader.loadNpmResources(new CanonicalType(measureUrl));

        verifyMeasure(measureUrl, expectedLibraryUrl, npmResourceInfoForCql);
        verifyLibrary(
                expectedLibraryUrl,
                expectedCql,
                npmResourceInfoForCql.getOptMainLibrary().orElse(null));
    }

    @Test
    void multiplePackages() {
        final NpmPackageLoaderInMemory loader = setup(
                Stream.of(SIMPLE_ALPHA_TGZ, SIMPLE_BRAVO_TGZ).map(Paths::get).toArray(Path[]::new));

        final NpmResourceInfoForCql resourceInfoAlpha = loader.loadNpmResources(new CanonicalType(MEASURE_URL_ALPHA));
        final NpmResourceInfoForCql resourceInfoBravo = loader.loadNpmResources(new CanonicalType(MEASURE_URL_BRAVO));

        verifyMeasure(MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA, resourceInfoAlpha);
        verifyLibrary(
                LIBRARY_URL_ALPHA,
                EXPECTED_CQL_ALPHA,
                resourceInfoAlpha.getOptMainLibrary().orElse(null));

        verifyMeasure(MEASURE_URL_BRAVO, LIBRARY_URL_BRAVO, resourceInfoBravo);
        verifyLibrary(
                LIBRARY_URL_BRAVO,
                EXPECTED_CQL_BRAVO,
                resourceInfoBravo.getOptMainLibrary().orElse(null));
    }

    @Test
    void derivedLibrary() {
        final Path tgzPath = WITH_DERIVED_LIBRARY_TGZ;
        final String measureUrl = MEASURE_URL_WITH_DERIVED_LIBRARY;
        final String withDerivedLibraryUrl = LIBRARY_URL_WITH_DERIVED_LIBRARY;
        final String derivedLibraryUrl = LIBRARY_URL_DERIVED_LIBRARY;
        final String libraryUrlWithVersion = withDerivedLibraryUrl + "|0.1";
        final String expectedCql = EXPECTED_CQL_WITH_DERIVED;
        final String expectedCqlDerived = EXPECTED_CQL_DERIVED;

        final NpmPackageLoaderInMemory loader = setup(tgzPath);

        final NpmResourceInfoForCql resourceInfo = loader.loadNpmResources(new CanonicalType(measureUrl));

        verifyMeasure(measureUrl, libraryUrlWithVersion, resourceInfo);
        verifyLibrary(
                withDerivedLibraryUrl,
                expectedCql,
                resourceInfo.getOptMainLibrary().orElse(null));

        final ILibraryAdapter derivedLibraryFromNoVersion = resourceInfo
                .findMatchingLibrary(new VersionedIdentifier().withId(DERIVED_LIBRARY_ID))
                .orElse(null);

        verifyLibrary(derivedLibraryUrl, expectedCqlDerived, derivedLibraryFromNoVersion);

        final ILibraryAdapter derivedLibraryFromVersion = resourceInfo
                .findMatchingLibrary(
                        new VersionedIdentifier().withId(DERIVED_LIBRARY_ID).withVersion("0.1"))
                .orElse(null);

        verifyLibrary(derivedLibraryUrl, expectedCqlDerived, derivedLibraryFromVersion);

        final ILibraryAdapter derivedLibraryFromBadVersion = resourceInfo
                .findMatchingLibrary(
                        new VersionedIdentifier().withId(DERIVED_LIBRARY_ID).withVersion("bad"))
                .orElse(null);

        assertNull(derivedLibraryFromBadVersion);
    }

    @Test
    void derivedLibraryTwoLayers() {
        final Path tgzPath = WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ;
        final String measureUrl = MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES;
        final String withDerivedLibraryUrl = LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES;
        final String derivedLibraryUrl1a = LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A;
        final String derivedLibraryUrl1b = LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B;
        final String derivedLibraryUrl2a = LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A;
        final String derivedLibraryUrl2b = LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B;
        final String libraryUrlWithVersion = withDerivedLibraryUrl + "|0.1";
        final String expectedCql = EXPECTED_CQL_DERIVED_TWO_LAYERS;
        final String expectedCqlDerived1a = EXPECTED_CQL_DERIVED_1_A;
        final String expectedCqlDerived1b = EXPECTED_CQL_DERIVED_1_B;
        final String expectedCqlDerived2a = EXPECTED_CQL_DERIVED_2_A;
        final String expectedCqlDerived2b = EXPECTED_CQL_DERIVED_2_B;

        final NpmPackageLoaderInMemory loader = setup(tgzPath);

        final NpmResourceInfoForCql resourceInfo = loader.loadNpmResources(new CanonicalType(measureUrl));

        verifyMeasure(measureUrl, libraryUrlWithVersion, resourceInfo);
        verifyLibrary(
                withDerivedLibraryUrl,
                expectedCql,
                resourceInfo.getOptMainLibrary().orElse(null));

        final ILibraryAdapter derivedLibrary1a = resourceInfo
                .findMatchingLibrary(
                        new VersionedIdentifier().withId("derived-layer-1a").withVersion("0.1.a"))
                .orElse(null);

        verifyLibrary(derivedLibraryUrl1a, expectedCqlDerived1a, derivedLibrary1a);

        final ILibraryAdapter derivedLibrary1b = resourceInfo
                .findMatchingLibrary(
                        new VersionedIdentifier().withId("derived-layer-1b").withVersion("0.1.b"))
                .orElse(null);

        verifyLibrary(derivedLibraryUrl1b, expectedCqlDerived1b, derivedLibrary1b);

        final ILibraryAdapter derivedLibrary2a = resourceInfo
                .findMatchingLibrary(
                        new VersionedIdentifier().withId("derived-layer-2a").withVersion("0.2.a"))
                .orElse(null);

        verifyLibrary(derivedLibraryUrl2a, expectedCqlDerived2a, derivedLibrary2a);

        final ILibraryAdapter derivedLibrary2b = resourceInfo
                .findMatchingLibrary(
                        new VersionedIdentifier().withId("derived-layer-2b").withVersion("0.2.b"))
                .orElse(null);

        verifyLibrary(derivedLibraryUrl2b, expectedCqlDerived2b, derivedLibrary2b);
    }

    private void verifyLibrary(String expectedLibraryUrl, String expectedCql, @Nullable ILibraryAdapter library) {
        assertNotNull(library);

        assertEquals(expectedLibraryUrl, library.getUrl());

        final List<Attachment> attachments = library.getContent();

        assertEquals(1, attachments.size());

        final Attachment attachment = attachments.get(0);

        assertEquals("text/cql", attachment.getContentType());
        final byte[] attachmentData = attachment.getData();
        final String cql = new String(attachmentData, StandardCharsets.UTF_8);

        assertEquals(expectedCql, cql);
    }

    private void verifyMeasure(
            String measureUrl, String expectedLibraryUrl, NpmResourceInfoForCql npmResourceInfoForCql) {
        final Optional<IMeasureAdapter> optMeasure = npmResourceInfoForCql.getMeasure();
        assertTrue(optMeasure.isPresent());
        final IMeasureAdapter measure = optMeasure.get();
        assertEquals(measureUrl, measure.getUrl());
        final List<String> libraryUrls = measure.getLibraryValues();
        assertEquals(1, libraryUrls.size());

        final String libraryUrl = libraryUrls.get(0);

        assertEquals(expectedLibraryUrl, libraryUrl);
    }

    @Nonnull
    private NpmPackageLoaderInMemory setup(Path... tgzPaths) {
        return NpmPackageLoaderInMemory.fromNpmPackageTgzPath(getClass(), tgzPaths);
    }
}
