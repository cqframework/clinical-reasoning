package org.opencds.cqf.fhir.utility.npm.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.adapter.r4.LibraryAdapter;
import org.opencds.cqf.fhir.utility.npm.BaseNpmPackageLoaderInMemory;
import org.opencds.cqf.fhir.utility.npm.BaseNpmPackageLoaderTest;

@SuppressWarnings("squid:S2699")
class NpmPackageLoaderR4Test extends BaseNpmPackageLoaderTest {

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
            """
        library opencds.withderivedlibrary WithDerivedLibrary version '0.4'

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers
        include DerivedLibrary version '0.4'

        parameter "Measurement Period" Interval<DateTime>
            default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)

        context Patient

        define "Initial Population":
            exists (DerivedLibrary."Encounter Finished")
        """;

    private static final String EXPECTED_CQL_DERIVED =
            """
        library opencds.withderivedlibrary.DerivedLibrary version '0.4'

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        context Patient

        define "Encounter Finished":
          [Encounter] E
            where E.status = 'finished'
        """;

    private static final String EXPECTED_CQL_DERIVED_TWO_LAYERS =
            """
        library opencds.withtwolayersderivedlibraries.WithTwoLayersDerivedLibraries version '0.5'

        using FHIR version '4.0.1'

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

        using FHIR version '4.0.1'

        include DerivedLayer2a version '0.5'
        include DerivedLayer2b version '0.5'

        context Patient

        define "Initial Population":
            DerivedLayer2a."Initial Population"
        """;

    private static final String EXPECTED_CQL_DERIVED_1_B =
            """
        library opencds.withtwolayersderivedlibraries.DerivedLayer1b version '0.5'

        using FHIR version '4.0.1'

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

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

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

        using FHIR version '4.0.1'

        include FHIRHelpers version '4.0.1' called FHIRHelpers

        parameter "Measurement Period" Interval<DateTime>
          default Interval[@2021-01-01T00:00:00.0-06:00, @2022-01-01T00:00:00.0-06:00)

        context Patient

        define "Numerator":
            exists ("Encounter Triaged")

        define "Encounter Triaged":
          [Encounter] E
            where E.status = 'triaged'
        """;

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

    @Test
    void simpleAlpha() {
        final BaseNpmPackageLoaderInMemory loader = setup(Path.of(SIMPLE_ALPHA_TGZ));

        final Optional<Measure> optMeasure =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_ALPHA));

        verifyMeasure(optMeasure.orElse(null), MEASURE_URL_ALPHA, VERSION_0_1, LIBRARY_URL_ALPHA_WITH_VERSION);

        final Optional<Library> optLibrary =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_ALPHA_WITH_VERSION));

        verifyLibrary(optLibrary.orElse(null), LIBRARY_URL_ALPHA_NO_VERSION, VERSION_0_1, EXPECTED_CQL_ALPHA);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(1, allNamespaceInfos.size());

        final NamespaceInfo namespaceInfo = allNamespaceInfos.get(0);

        assertEquals(SIMPLE_ALPHA_NAMESPACE, namespaceInfo.getName());
        assertEquals(SIMPLE_ALPHA_NAMESPACE_URL, namespaceInfo.getUri());
    }

    @Test
    void simpleBravo() {
        final BaseNpmPackageLoaderInMemory loader = setup(Path.of(SIMPLE_BRAVO_TGZ));

        final Optional<Measure> optMeasure =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_BRAVO));

        verifyMeasure(optMeasure.orElse(null), MEASURE_URL_BRAVO, VERSION_0_1, LIBRARY_URL_BRAVO_WITH_VERSION);

        final Optional<Library> optLibrary =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_BRAVO_WITH_VERSION));

        verifyLibrary(optLibrary.orElse(null), LIBRARY_URL_BRAVO_NO_VERSION, VERSION_0_1, EXPECTED_CQL_BRAVO);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(1, allNamespaceInfos.size());

        final NamespaceInfo namespaceInfo = allNamespaceInfos.get(0);

        assertEquals(SIMPLE_BRAVO_NAMESPACE, namespaceInfo.getName());
        assertEquals(SIMPLE_BRAVO_NAMESPACE_URL, namespaceInfo.getUri());
    }

    @Test
    void multiplePackages() {
        final BaseNpmPackageLoaderInMemory loader = setup(
                Stream.of(SIMPLE_ALPHA_TGZ, SIMPLE_BRAVO_TGZ).map(Paths::get).toList());

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(2, allNamespaceInfos.size());

        assertTrue(allNamespaceInfos.contains(new NamespaceInfo(SIMPLE_ALPHA_NAMESPACE, SIMPLE_ALPHA_NAMESPACE_URL)));
        assertTrue(allNamespaceInfos.contains(new NamespaceInfo(SIMPLE_BRAVO_NAMESPACE, SIMPLE_BRAVO_NAMESPACE_URL)));

        final Optional<Measure> optMeasureAlpha =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_ALPHA));

        verifyMeasure(optMeasureAlpha.orElse(null), MEASURE_URL_ALPHA, VERSION_0_1, LIBRARY_URL_ALPHA_WITH_VERSION);

        final Optional<Library> optLibraryAlpha =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_ALPHA_WITH_VERSION));

        verifyLibrary(optLibraryAlpha.orElse(null), LIBRARY_URL_ALPHA_NO_VERSION, VERSION_0_1, EXPECTED_CQL_ALPHA);

        final Optional<Measure> optMeasureBravo =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_ALPHA));

        verifyMeasure(optMeasureBravo.orElse(null), MEASURE_URL_ALPHA, VERSION_0_1, LIBRARY_URL_ALPHA_WITH_VERSION);

        final Optional<Library> optLibraryBravo =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_BRAVO_WITH_VERSION));

        verifyLibrary(optLibraryBravo.orElse(null), LIBRARY_URL_BRAVO_NO_VERSION, VERSION_0_1, EXPECTED_CQL_BRAVO);
    }

    @Test
    void derivedLibrary() {

        final BaseNpmPackageLoaderInMemory loader = setup(WITH_DERIVED_LIBRARY_TGZ);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(1, allNamespaceInfos.size());

        assertTrue(allNamespaceInfos.contains(new NamespaceInfo(WITH_DERIVED_NAMESPACE, WITH_DERIVED_URL)));

        final Optional<Measure> optMeasureWithDerived =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_WITH_DERIVED_LIBRARY));

        verifyMeasure(
                optMeasureWithDerived.orElse(null),
                MEASURE_URL_WITH_DERIVED_LIBRARY,
                VERSION_0_4,
                LIBRARY_URL_WITH_DERIVED_LIBRARY_WITH_VERSION);

        final Optional<Library> optLibraryWithDerived =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_WITH_DERIVED_LIBRARY_WITH_VERSION));

        verifyLibrary(
                optLibraryWithDerived.orElse(null),
                LIBRARY_URL_WITH_DERIVED_LIBRARY_NO_VERSION,
                VERSION_0_4,
                EXPECTED_CQL_WITH_DERIVED);

        final Optional<Library> optLibraryDerived =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_DERIVED_LIBRARY));

        verifyLibrary(optLibraryDerived.orElse(null), LIBRARY_URL_DERIVED_LIBRARY, VERSION_0_4, EXPECTED_CQL_DERIVED);
    }

    @Test
    void derivedLibraryTwoLayers() {

        final BaseNpmPackageLoaderInMemory loader = setup(WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(1, allNamespaceInfos.size());

        assertTrue(
                allNamespaceInfos.contains(new NamespaceInfo(WITH_TWO_LAYERS_NAMESPACE, WITH_TWO_LAYERS_DERIVED_URL)));

        final Optional<Measure> optMeasureWithTwoDerived =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES));

        verifyMeasure(
                optMeasureWithTwoDerived.orElse(null),
                MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES,
                VERSION_0_5,
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION);

        final Optional<Library> optLibraryWithTwoDerived = loader.loadNpmResource(
                Library.class, new CanonicalType(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION));

        verifyLibrary(
                optLibraryWithTwoDerived.orElse(null),
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_TWO_LAYERS);

        final Optional<Library> optLibraryWithTwoDerived1a = loader.loadNpmResource(
                Library.class, new CanonicalType(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A));

        verifyLibrary(
                optLibraryWithTwoDerived1a.orElse(null),
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_1_A);

        final Optional<Library> optLibraryWithTwoDerived1b = loader.loadNpmResource(
                Library.class, new CanonicalType(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B));

        verifyLibrary(
                optLibraryWithTwoDerived1b.orElse(null),
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_1_B);

        final Optional<Library> optLibraryWithTwoDerived2a = loader.loadNpmResource(
                Library.class, new CanonicalType(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A));

        verifyLibrary(
                optLibraryWithTwoDerived2a.orElse(null),
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_2_A);

        final Optional<Library> optLibraryWithTwoDerived2b = loader.loadNpmResource(
                Library.class, new CanonicalType(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B));

        verifyLibrary(
                optLibraryWithTwoDerived2b.orElse(null),
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_2_B);

        final ILibraryAdapter libraryAdapter1a = loader.findMatchingLibrary(new VersionedIdentifier()
                        .withId(DERIVED_LAYER_1_A)
                        .withVersion(VERSION_0_5)
                        .withSystem(WITH_TWO_LAYERS_DERIVED_URL))
                .orElse(null);

        verifyLibrary(
                libraryAdapter1a,
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_1_A);

        final ILibraryAdapter libraryAdapter1b = loader.findMatchingLibrary(new VersionedIdentifier()
                        .withId(DERIVED_LAYER_1_B)
                        .withVersion(VERSION_0_5)
                        .withSystem(WITH_TWO_LAYERS_DERIVED_URL))
                .orElse(null);

        verifyLibrary(
                libraryAdapter1b,
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_1_B);

        final ILibraryAdapter libraryAdapter2a = loader.findMatchingLibrary(new VersionedIdentifier()
                        .withId(DERIVED_LAYER_2_A)
                        .withVersion(VERSION_0_5)
                        .withSystem(WITH_TWO_LAYERS_DERIVED_URL))
                .orElse(null);

        verifyLibrary(
                libraryAdapter2a,
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_2_A);

        final ILibraryAdapter libraryAdapter2b = loader.findMatchingLibrary(new VersionedIdentifier()
                        .withId(DERIVED_LAYER_2_B)
                        .withVersion(VERSION_0_5)
                        .withSystem(WITH_TWO_LAYERS_DERIVED_URL))
                .orElse(null);

        verifyLibrary(
                libraryAdapter2b,
                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B,
                VERSION_0_5,
                EXPECTED_CQL_DERIVED_2_B);
    }

    @Test
    void crossPackage() {

        final BaseNpmPackageLoaderInMemory loader = setup(CROSS_PACKAGE_SOURCE_TGZ, CROSS_PACKAGE_TARGET_TGZ);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(2, allNamespaceInfos.size());

        assertTrue(allNamespaceInfos.contains(
                new NamespaceInfo(CROSS_PACKAGE_SOURCE_NAMESPACE, CROSS_PACKAGE_SOURCE_URL)));
        assertTrue(allNamespaceInfos.contains(
                new NamespaceInfo(CROSS_PACKAGE_TARGET_NAMESPACE, CROSS_PACKAGE_TARGET_URL)));

        final Optional<Measure> optMeasureCrossSource =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_CROSS_PACKAGE_SOURCE));

        verifyMeasure(
                optMeasureCrossSource.orElse(null),
                MEASURE_URL_CROSS_PACKAGE_SOURCE,
                VERSION_0_2,
                LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION);

        final Optional<Library> optLibraryCrossSource =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION));

        verifyLibrary(
                optLibraryCrossSource.orElse(null),
                LIBRARY_URL_CROSS_PACKAGE_SOURCE_NO_VERSION,
                VERSION_0_2,
                EXPECTED_CQL_CROSS_SOURCE);

        final Optional<Measure> optMeasureCrossTarget =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_CROSS_PACKAGE_TARGET));

        verifyMeasure(
                optMeasureCrossTarget.orElse(null),
                MEASURE_URL_CROSS_PACKAGE_TARGET,
                VERSION_0_3,
                LIBRARY_URL_CROSS_PACKAGE_TARGET_WITH_VERSION);

        final Optional<Library> optLibraryCrossTarget =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_CROSS_PACKAGE_TARGET_WITH_VERSION));

        verifyLibrary(
                optLibraryCrossTarget.orElse(null),
                LIBRARY_URL_CROSS_PACKAGE_TARGET_NO_VERSION,
                VERSION_0_3,
                EXPECTED_CQL_CROSS_TARGET);

        final ILibraryAdapter libraryAdapterSource = loader.findMatchingLibrary(new VersionedIdentifier()
                        .withId(CROSS_PACKAGE_SOURCE_ID)
                        .withVersion(VERSION_0_2)
                        .withSystem(CROSS_PACKAGE_SOURCE_URL))
                .orElse(null);

        verifyLibrary(
                libraryAdapterSource,
                LIBRARY_URL_CROSS_PACKAGE_SOURCE_NO_VERSION,
                VERSION_0_2,
                EXPECTED_CQL_CROSS_SOURCE);

        final ILibraryAdapter libraryAdapterTarget = loader.findMatchingLibrary(new VersionedIdentifier()
                        .withId(CROSS_PACKAGE_TARGET_ID)
                        .withVersion(VERSION_0_3)
                        .withSystem(CROSS_PACKAGE_TARGET_URL))
                .orElse(null);

        verifyLibrary(
                libraryAdapterTarget,
                LIBRARY_URL_CROSS_PACKAGE_TARGET_NO_VERSION,
                VERSION_0_3,
                EXPECTED_CQL_CROSS_TARGET);
    }

    protected void verifyMeasure(
            @Nullable Measure measure, String measureUrl, String version, String expectedLibraryUrl) {

        assertNotNull(measure, "Could not find measure with url: %s".formatted(measureUrl));

        assertEquals(measureUrl, measure.getUrl());

        assertEquals(version, measure.getVersion());

        final List<CanonicalType> libraryUrls = measure.getLibrary();
        assertEquals(1, libraryUrls.size());
        final CanonicalType libraryUrl = libraryUrls.get(0);
        assertEquals(expectedLibraryUrl, libraryUrl.asStringValue());
    }

    private void verifyLibrary(
            @Nullable ILibraryAdapter libraryAdapter, String expectedLibraryUrl, String version, String expectedCql) {

        assertNotNull(libraryAdapter, "Could not find library with url: %s".formatted(expectedLibraryUrl));

        assertInstanceOf(LibraryAdapter.class, libraryAdapter);

        verifyLibrary(((LibraryAdapter) libraryAdapter).get(), expectedLibraryUrl, version, expectedCql);
    }

    private void verifyLibrary(
            @Nullable Library library, String expectedLibraryUrl, String version, String expectedCql) {

        assertNotNull(library, "Could not find library with url: %s".formatted(expectedLibraryUrl));

        assertEquals(expectedLibraryUrl, library.getUrl());

        assertEquals(version, library.getVersion());

        final List<Attachment> attachments = library.getContent();
        assertEquals(1, attachments.size());
        final Attachment attachment = attachments.get(0);
        assertEquals("text/cql", attachment.getContentType());
        final byte[] attachmentData = attachment.getData();
        final String cql = new String(attachmentData, StandardCharsets.UTF_8);

        assertEquals(expectedCql, cql);
    }

    @Nonnull
    @Override
    protected BaseNpmPackageLoaderInMemory setup(Path... npmPackagePaths) {
        return R4NpmPackageLoaderInMemory.fromNpmPackageClasspath(getClass(), npmPackagePaths);
    }

    @Nonnull
    @Override
    protected BaseNpmPackageLoaderInMemory setup(List<Path> npmPackagePaths) {
        return R4NpmPackageLoaderInMemory.fromNpmPackageClasspath(getClass(), npmPackagePaths);
    }
}
