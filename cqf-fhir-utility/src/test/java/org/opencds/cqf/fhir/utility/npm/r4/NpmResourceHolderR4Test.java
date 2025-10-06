package org.opencds.cqf.fhir.utility.npm.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.npm.BaseNpmResourceInfoForCqlTest;
import org.opencds.cqf.fhir.utility.npm.R4NpmPackageLoaderInMemory;

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

    @Override
    protected FhirVersionEnum getExpectedFhirVersion() {
        return FhirVersionEnum.R4;
    }

    @Test
    void simpleAlpha() {
        final R4NpmPackageLoaderInMemory loader = setup(Path.of(SIMPLE_ALPHA_TGZ));

        final Optional<Measure> optMeasure =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_ALPHA));

        verifyMeasure(optMeasure.orElse(null), MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA_WITH_VERSION);

        final Optional<Library> optLibrary =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_ALPHA_WITH_VERSION));

        verifyLibrary(optLibrary.orElse(null), LIBRARY_URL_ALPHA_NO_VERSION, EXPECTED_CQL_ALPHA);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(1, allNamespaceInfos.size());

        final NamespaceInfo namespaceInfo = allNamespaceInfos.get(0);

        assertEquals(SIMPLE_ALPHA_NAMESPACE, namespaceInfo.getName());
        assertEquals(SIMPLE_ALPHA_NAMESPACE_URL, namespaceInfo.getUri());
    }

    @Test
    void simpleBravo() {
        final R4NpmPackageLoaderInMemory loader = setup(Path.of(SIMPLE_BRAVO_TGZ));

        final Optional<Measure> optMeasure =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_BRAVO));

        verifyMeasure(optMeasure.orElse(null), MEASURE_URL_BRAVO, LIBRARY_URL_BRAVO_WITH_VERSION);

        final Optional<Library> optLibrary =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_BRAVO_WITH_VERSION));

        verifyLibrary(optLibrary.orElse(null), LIBRARY_URL_BRAVO_NO_VERSION, EXPECTED_CQL_BRAVO);

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(1, allNamespaceInfos.size());

        final NamespaceInfo namespaceInfo = allNamespaceInfos.get(0);

        assertEquals(SIMPLE_BRAVO_NAMESPACE, namespaceInfo.getName());
        assertEquals(SIMPLE_BRAVO_NAMESPACE_URL, namespaceInfo.getUri());
    }

    @Test
    void multiplePackages() {
        final R4NpmPackageLoaderInMemory loader = R4NpmPackageLoaderInMemory.fromNpmPackageClasspath(
                getClass(),
                Stream.of(SIMPLE_ALPHA_TGZ, SIMPLE_BRAVO_TGZ).map(Paths::get).toList());

        final List<NamespaceInfo> allNamespaceInfos =
                loader.getNamespaceManager().getAllNamespaceInfos();

        assertEquals(2, allNamespaceInfos.size());

        assertTrue(allNamespaceInfos.contains(new NamespaceInfo(SIMPLE_ALPHA_NAMESPACE, SIMPLE_ALPHA_NAMESPACE_URL)));
        assertTrue(allNamespaceInfos.contains(new NamespaceInfo(SIMPLE_BRAVO_NAMESPACE, SIMPLE_BRAVO_NAMESPACE_URL)));

        final Optional<Measure> optMeasureAlpha =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_ALPHA));

        verifyMeasure(optMeasureAlpha.orElse(null), MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA_WITH_VERSION);

        final Optional<Library> optLibraryAlpha =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_ALPHA_WITH_VERSION));

        verifyLibrary(optLibraryAlpha.orElse(null), LIBRARY_URL_ALPHA_NO_VERSION, EXPECTED_CQL_ALPHA);

        final Optional<Measure> optMeasureBravo =
                loader.loadNpmResource(Measure.class, new CanonicalType(MEASURE_URL_ALPHA));

        verifyMeasure(optMeasureBravo.orElse(null), MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA_WITH_VERSION);

        final Optional<Library> optLibraryBravo =
                loader.loadNpmResource(Library.class, new CanonicalType(LIBRARY_URL_BRAVO_WITH_VERSION));

        verifyLibrary(optLibraryBravo.orElse(null), LIBRARY_URL_BRAVO_NO_VERSION, EXPECTED_CQL_BRAVO);
    }

    protected void verifyMeasure(@Nullable Measure measure, String measureUrl, String expectedLibraryUrl) {

        assertNotNull(measure, "Could not find measure with url: %s".formatted(measureUrl));

        assertEquals(measureUrl, measure.getUrl());

        final List<CanonicalType> libraryUrls = measure.getLibrary();
        assertEquals(1, libraryUrls.size());
        final CanonicalType libraryUrl = libraryUrls.get(0);
        assertEquals(expectedLibraryUrl, libraryUrl.asStringValue());
    }

    private void verifyLibrary(@Nullable Library library, String expectedLibraryUrl, String expectedCql) {

        assertNotNull(library, "Could not find library with url: %s".formatted(expectedLibraryUrl));

        assertEquals(expectedLibraryUrl, library.getUrl());

        final List<Attachment> attachments = library.getContent();

        assertEquals(1, attachments.size());

        final Attachment attachment = attachments.get(0);

        assertEquals("text/cql", attachment.getContentType());
        final byte[] attachmentData = attachment.getData();
        final String cql = new String(attachmentData, StandardCharsets.UTF_8);

        assertEquals(expectedCql, cql);
    }

    //    @Test
    //    void multiplePackages() {
    //        multiplePackages(EXPECTED_CQL_ALPHA, EXPECTED_CQL_BRAVO);
    //    }
    //
    //    @Test
    //    void derivedLibrary() {
    //        derivedLibrary(EXPECTED_CQL_WITH_DERIVED, EXPECTED_CQL_DERIVED);
    //    }
    //
    //    @Test
    //    void derivedLibraryTwoLayers() {
    //        derivedLibraryTwoLayers(
    //                EXPECTED_CQL_DERIVED_TWO_LAYERS,
    //                EXPECTED_CQL_DERIVED_1_A,
    //                EXPECTED_CQL_DERIVED_1_B,
    //                EXPECTED_CQL_DERIVED_2_A,
    //                EXPECTED_CQL_DERIVED_2_B);
    //    }
    //
    //    @Test
    //    void crossPackage() {
    //        crossPackage(EXPECTED_CQL_CROSS_SOURCE, EXPECTED_CQL_CROSS_TARGET);
    //    }
}
