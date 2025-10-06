package org.opencds.cqf.fhir.utility.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAttachmentAdapter;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

public abstract class BaseNpmResourceInfoForCqlTest {

    protected static final String DOT_TGZ = ".tgz";

    protected static final String SIMPLE_ALPHA_LOWER = "simplealpha";
    protected static final String SIMPLE_ALPHA_MIXED = "SimpleAlpha";
    protected static final String SIMPLE_BRAVO_LOWER = "simplebravo";
    protected static final String SIMPLE_BRAVO_MIXED = "SimpleBravo";
    protected static final String WITH_DERIVED_LIBRARY_LOWER = "withderivedlibrary";
    protected static final String WITH_DERIVED_LIBRARY_MIXED = "WithDerivedLibrary";
    protected static final String DERIVED_LIBRARY_ID = "DerivedLibrary";
    protected static final String DERIVED_LIBRARY = DERIVED_LIBRARY_ID;

    protected static final String NAMESPACE_PREFIX = "opencds.";

    protected static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES = "withtwolayersderivedlibraries";
    protected static final String WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER = "WithTwoLayersDerivedLibraries";

    protected static final String SIMPLE_ALPHA_NAMESPACE = NAMESPACE_PREFIX + SIMPLE_ALPHA_LOWER;
    protected static final String SIMPLE_BRAVO_NAMESPACE = NAMESPACE_PREFIX + SIMPLE_BRAVO_LOWER;
    protected static final String WITH_DERIVED_NAMESPACE = NAMESPACE_PREFIX + WITH_DERIVED_LIBRARY_LOWER;
    protected static final String WITH_TWO_LAYERS_NAMESPACE = NAMESPACE_PREFIX + WITH_TWO_LAYERS_DERIVED_LIBRARIES;

    protected static final String DERIVED_LAYER_1_A = "DerivedLayer1a";
    protected static final String DERIVED_LAYER_1_B = "DerivedLayer1b";
    protected static final String DERIVED_LAYER_2_A = "DerivedLayer2a";
    protected static final String DERIVED_LAYER_2_B = "DerivedLayer2b";
    protected static final String CROSS_PACKAGE_SOURCE = "crosspackagesource";
    protected static final String CROSS_PACKAGE_SOURCE_ID = "CrossPackageSource";
    protected static final String CROSS_PACKAGE_TARGET = "crosspackagetarget";
    protected static final String CROSS_PACKAGE_TARGET_ID = "CrossPackageTarget";

    protected static final String SIMPLE_ALPHA_TGZ = SIMPLE_ALPHA_LOWER + DOT_TGZ;
    protected static final String SIMPLE_BRAVO_TGZ = SIMPLE_BRAVO_LOWER + DOT_TGZ;
    protected static final Path WITH_DERIVED_LIBRARY_TGZ = Paths.get(WITH_DERIVED_LIBRARY_LOWER + DOT_TGZ);
    protected static final Path WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ =
            Paths.get(WITH_TWO_LAYERS_DERIVED_LIBRARIES + DOT_TGZ);
    protected static final Path CROSS_PACKAGE_SOURCE_TGZ = Paths.get(CROSS_PACKAGE_SOURCE + DOT_TGZ);
    protected static final Path CROSS_PACKAGE_TARGET_TGZ = Paths.get(CROSS_PACKAGE_TARGET + DOT_TGZ);

    protected static final String SLASH_MEASURE_SLASH = "/Measure/";
    protected static final String SLASH_LIBRARY_SLASH = "/Library/";

    private static final String PIPE = "|";
    private static final String VERSION_0_1 = "0.1";
    private static final String VERSION_0_2 = "0.2";
    private static final String VERSION_0_4 = "0.4";
    private static final String VERSION_0_5 = "0.5";

    protected static final String SIMPLE_ALPHA_NAMESPACE_URL = "http://simplealpha.npm.opencds.org";
    protected static final String SIMPLE_BRAVO_NAMESPACE_URL = "http://simplebravo.npm.opencds.org";
    protected static final String WITH_DERIVED_URL = "http://withderivedlibrary.npm.opencds.org";
    protected static final String WITH_DERIVED_TWO_LAYERS_URL = "http://withtwolayersderivedlibraries.npm.opencds.org";
    protected static final String CROSS_PACKAGE_SOURCE_URL = "http://crosspackagesource.npm.opencds.org";
    protected static final String CROSS_PACKAGE_TARGET_URL = "http://crosspackagetarget.npm.opencds.org";

    protected static final String MEASURE_URL_ALPHA =
            SIMPLE_ALPHA_NAMESPACE_URL + SLASH_MEASURE_SLASH + SIMPLE_ALPHA_MIXED;
    protected static final String MEASURE_URL_BRAVO =
            SIMPLE_BRAVO_NAMESPACE_URL + SLASH_MEASURE_SLASH + SIMPLE_BRAVO_MIXED;
    protected static final String MEASURE_URL_WITH_DERIVED_LIBRARY =
            WITH_DERIVED_URL + SLASH_MEASURE_SLASH + WITH_DERIVED_LIBRARY_MIXED;
    protected static final String MEASURE_URL_WITH_DERIVED_LIBRARY_WITH_VERSION =
            MEASURE_URL_WITH_DERIVED_LIBRARY + PIPE + VERSION_0_4;
    protected static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES =
            WITH_DERIVED_TWO_LAYERS_URL + SLASH_MEASURE_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER;
    protected static final String MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION =
            MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES + PIPE + VERSION_0_5;

    protected static final String MEASURE_URL_CROSS_PACKAGE_SOURCE =
            CROSS_PACKAGE_SOURCE_URL + SLASH_MEASURE_SLASH + CROSS_PACKAGE_SOURCE_ID;
    protected static final String MEASURE_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION =
            MEASURE_URL_CROSS_PACKAGE_SOURCE + PIPE + VERSION_0_2;

    protected static final String LIBRARY_URL_ALPHA_NO_VERSION =
            SIMPLE_ALPHA_NAMESPACE_URL + SLASH_LIBRARY_SLASH + SIMPLE_ALPHA_MIXED;
    protected static final String LIBRARY_URL_ALPHA_WITH_VERSION = LIBRARY_URL_ALPHA_NO_VERSION + PIPE + VERSION_0_1;
    protected static final String LIBRARY_URL_BRAVO_NO_VERSION =
            SIMPLE_BRAVO_NAMESPACE_URL + SLASH_LIBRARY_SLASH + SIMPLE_BRAVO_MIXED;
    protected static final String LIBRARY_URL_BRAVO_WITH_VERSION = LIBRARY_URL_BRAVO_NO_VERSION + PIPE + VERSION_0_1;

    protected static final String LIBRARY_URL_WITH_DERIVED_LIBRARY_NO_VERSION =
            WITH_DERIVED_URL + SLASH_LIBRARY_SLASH + WITH_DERIVED_LIBRARY_MIXED;
    protected static final String LIBRARY_URL_WITH_DERIVED_LIBRARY_WITH_VERSION =
            WITH_DERIVED_URL + SLASH_LIBRARY_SLASH + WITH_DERIVED_LIBRARY_MIXED + PIPE + VERSION_0_4;
    protected static final String LIBRARY_URL_DERIVED_LIBRARY =
            WITH_DERIVED_URL + SLASH_LIBRARY_SLASH + DERIVED_LIBRARY;

    protected static final String LIBRARY_URL_CROSS_PACKAGE_SOURCE =
            CROSS_PACKAGE_SOURCE_URL + SLASH_LIBRARY_SLASH + CROSS_PACKAGE_SOURCE_ID;
    protected static final String LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION =
            LIBRARY_URL_CROSS_PACKAGE_SOURCE + PIPE + VERSION_0_2;
    protected static final String LIBRARY_URL_CROSS_PACKAGE_TARGET =
            CROSS_PACKAGE_TARGET_URL + SLASH_LIBRARY_SLASH + CROSS_PACKAGE_TARGET_ID;

    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION =
            WITH_DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + WITH_TWO_LAYERS_DERIVED_LIBRARIES_UPPER;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION =
            LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION + PIPE + VERSION_0_5;

    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A =
            WITH_DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_1_A;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B =
            WITH_DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_1_B;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A =
            WITH_DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_2_A;
    protected static final String LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B =
            WITH_DERIVED_TWO_LAYERS_URL + SLASH_LIBRARY_SLASH + DERIVED_LAYER_2_B;

    // LUKETODO:
    protected abstract FhirVersionEnum getExpectedFhirVersion();

    //    protected void simpleAlpha(
    //            Path tgzPath,
    //            String measureUrl,
    //            String expectedLibraryUrlFromMeasure,
    //            String expectedLibraryUrlWithinLibrary,
    //            String expectedCql) {
    //
    //        simpleCommon(
    //                tgzPath,
    //                List.of(new NamespaceInfo(SIMPLE_ALPHA_NAMESPACE, SIMPLE_ALPHA_NAMESPACE_URL)),
    //                measureUrl,
    //                expectedLibraryUrlFromMeasure,
    //                expectedLibraryUrlWithinLibrary,
    //                expectedCql);
    //    }
    //
    //    protected void simpleBravo(
    //            Path tgzPath,
    //            String measureUrl,
    //            String expectedLibraryUrlFromMeasure,
    //            String expectedLibraryUrlWithinLibrary,
    //            String expectedCql) {
    //
    //        simpleCommon(
    //                tgzPath,
    //                List.of(new NamespaceInfo(SIMPLE_BRAVO_NAMESPACE, SIMPLE_BRAVO_NAMESPACE_URL)),
    //                measureUrl,
    //                expectedLibraryUrlFromMeasure,
    //                expectedLibraryUrlWithinLibrary,
    //                expectedCql);
    //    }

    //    protected void simpleCommon(
    //            Path tgzPath,
    //            List<NamespaceInfo> expectedNamespaceInfos,
    //            String measureUrl,
    //            String expectedLibraryUrlFromMeasure,
    //            String expectedLibraryUrlWithinLibrary,
    //            String expectedCql) {
    //        final R4NpmPackageLoaderInMemory loader = setup(tgzPath);
    //
    //        final Optional<Measure> optMeasure =
    //                loader.loadNpmResource(Measure.class, new CanonicalType(measureUrl));
    //
    //        verifyMeasure(measureUrl, expectedLibraryUrlFromMeasure, optMeasure);
    //        verifyLibrary(
    //                expectedLibraryUrlWithinLibrary,
    //                expectedCql,
    //                npmResourceHolder.getOptMainLibrary().orElse(null));
    //    }
    //
    //    protected void multiplePackages(String expectedCqlAlpha, String expectedCqlBravo) {
    //        final R4NpmPackageLoaderInMemory loader = setup(
    //                Stream.of(SIMPLE_ALPHA_TGZ, SIMPLE_BRAVO_TGZ).map(Paths::get).toArray(Path[]::new));
    //
    //        final Optional<Measure> optMeasureAlpha =
    //                loader.loadNpmResource(
    //                    Measure.class,
    //                    new CanonicalType(MEASURE_URL_ALPHA));
    //        final Optional<Measure> optMeasureBravo =
    //                loader.loadNpmResource(
    //                    Measure.class,
    //                    new CanonicalType(MEASURE_URL_BRAVO));
    //
    //        assertEquals(
    //                List.of(new NamespaceInfo(SIMPLE_ALPHA_NAMESPACE, SIMPLE_ALPHA_NAMESPACE_URL)),
    //                resourceInfoAlpha.getNamespaceInfos());
    //        assertEquals(
    //                List.of(new NamespaceInfo(SIMPLE_BRAVO_NAMESPACE, SIMPLE_BRAVO_NAMESPACE_URL)),
    //                resourceInfoBravo.getNamespaceInfos());
    //
    //        verifyMeasure(MEASURE_URL_ALPHA, LIBRARY_URL_ALPHA_WITH_VERSION, resourceInfoAlpha);
    //        verifyLibrary(
    //                LIBRARY_URL_ALPHA_NO_VERSION,
    //                expectedCqlAlpha,
    //                resourceInfoAlpha.getOptMainLibrary().orElse(null));
    //
    //        verifyMeasure(MEASURE_URL_BRAVO, LIBRARY_URL_BRAVO_WITH_VERSION, resourceInfoBravo);
    //        verifyLibrary(
    //                LIBRARY_URL_BRAVO_NO_VERSION,
    //                expectedCqlBravo,
    //                resourceInfoBravo.getOptMainLibrary().orElse(null));
    //    }

    //    protected void derivedLibrary(String expectedCql, String expectedCqlDerived) {
    //
    //        final R4NpmPackageLoaderInMemory loader = setup(WITH_DERIVED_LIBRARY_TGZ);
    //
    //        final NpmResourceHolder resourceInfoWithNoVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_WITH_DERIVED_LIBRARY));
    //
    //        sanityCheckNpmResourceHolder(resourceInfoWithNoVersion);
    //
    //        var expectedNamespaceInfos = List.of(new NamespaceInfo(WITH_DERIVED_NAMESPACE, WITH_DERIVED_URL));
    //
    //        assertEquals(expectedNamespaceInfos, resourceInfoWithNoVersion.getNamespaceInfos());
    //
    //        verifyMeasure(
    //                MEASURE_URL_WITH_DERIVED_LIBRARY,
    //                LIBRARY_URL_WITH_DERIVED_LIBRARY_WITH_VERSION,
    //                resourceInfoWithNoVersion);
    //
    //        final NpmResourceHolder resourceInfoWithVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_WITH_DERIVED_LIBRARY_WITH_VERSION));
    //        sanityCheckNpmResourceHolder(resourceInfoWithVersion);
    //        assertEquals(expectedNamespaceInfos, resourceInfoWithVersion.getNamespaceInfos());
    //
    //        verifyMeasure(
    //                MEASURE_URL_WITH_DERIVED_LIBRARY,
    //                LIBRARY_URL_WITH_DERIVED_LIBRARY_WITH_VERSION,
    //                resourceInfoWithVersion);
    //        verifyLibrary(
    //                LIBRARY_URL_WITH_DERIVED_LIBRARY_NO_VERSION,
    //                expectedCql,
    //                resourceInfoWithVersion.getOptMainLibrary().orElse(null));
    //
    //        final ILibraryAdapter derivedLibraryFromNoVersion = resourceInfoWithVersion
    //                .findMatchingLibrary(new VersionedIdentifier().withId(DERIVED_LIBRARY_ID))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_DERIVED_LIBRARY, expectedCqlDerived, derivedLibraryFromNoVersion);
    //
    //        final ILibraryAdapter derivedLibraryFromVersion = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LIBRARY_ID).withVersion("0.4"))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_DERIVED_LIBRARY, expectedCqlDerived, derivedLibraryFromVersion);
    //
    //        final ILibraryAdapter derivedLibraryFromBadVersion = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LIBRARY_ID).withVersion("bad"))
    //                .orElse(null);
    //
    //        assertNull(derivedLibraryFromBadVersion);
    //    }

    //    protected void derivedLibraryTwoLayers(
    //            String expectedCql,
    //            String expectedCqlDerived1a,
    //            String expectedCqlDerived1b,
    //            String expectedCqlDerived2a,
    //            String expectedCqlDerived2b) {
    //
    //        final R4NpmPackageLoaderInMemory loader = setup(WITH_TWO_LAYERS_DERIVED_LIBRARIES_TGZ);
    //
    //        var expectedNamespaceInfos = List.of(new NamespaceInfo(WITH_TWO_LAYERS_NAMESPACE,
    // WITH_DERIVED_TWO_LAYERS_URL));
    //
    //        final NpmResourceHolder resourceInfoNoVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES));
    //        sanityCheckNpmResourceHolder(resourceInfoNoVersion);
    //        assertEquals(expectedNamespaceInfos, resourceInfoNoVersion.getNamespaceInfos());
    //        verifyMeasure(
    //                MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES,
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION,
    //                resourceInfoNoVersion);
    //        verifyLibrary(
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION,
    //                expectedCql,
    //                resourceInfoNoVersion.getOptMainLibrary().orElse(null));
    //
    //        final NpmResourceHolder resourceInfoWithVersion =
    //                loader.loadNpmResources(new
    // CanonicalType(MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION));
    //        sanityCheckNpmResourceHolder(resourceInfoWithVersion);
    //        assertEquals(expectedNamespaceInfos, resourceInfoWithVersion.getNamespaceInfos());
    //        verifyMeasure(
    //                MEASURE_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES,
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_WITH_VERSION,
    //                resourceInfoWithVersion);
    //        verifyLibrary(
    //                LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARIES_NO_VERSION,
    //                expectedCql,
    //                resourceInfoWithVersion.getOptMainLibrary().orElse(null));
    //
    //        final ILibraryAdapter derivedLibrary1a = resourceInfoWithVersion
    //                .findMatchingLibrary(new VersionedIdentifier()
    //                        .withId(DERIVED_LAYER_1_A)
    //                        .withVersion(VERSION_0_5)
    //                        .withSystem(WITH_DERIVED_TWO_LAYERS_URL))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1A, expectedCqlDerived1a, derivedLibrary1a);
    //
    //        final ILibraryAdapter derivedLibrary1b = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LAYER_1_B).withVersion(VERSION_0_5))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_1B, expectedCqlDerived1b, derivedLibrary1b);
    //
    //        final ILibraryAdapter derivedLibrary2a = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LAYER_2_A).withVersion(VERSION_0_5))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2A, expectedCqlDerived2a, derivedLibrary2a);
    //
    //        final ILibraryAdapter derivedLibrary2b = resourceInfoWithVersion
    //                .findMatchingLibrary(
    //                        new VersionedIdentifier().withId(DERIVED_LAYER_2_B).withVersion(VERSION_0_5))
    //                .orElse(null);
    //
    //        verifyLibrary(LIBRARY_URL_WITH_TWO_LAYERS_DERIVED_LIBRARY_2B, expectedCqlDerived2b, derivedLibrary2b);
    //    }

    //    protected void crossPackage(String expectedCqlSource, String expectedCqlTarget) {
    //
    //        final R4NpmPackageLoaderInMemory loader = setup(CROSS_PACKAGE_SOURCE_TGZ, CROSS_PACKAGE_TARGET_TGZ);
    //
    //        final NpmResourceHolder resourceInfoWithNoVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_CROSS_PACKAGE_SOURCE));
    //        sanityCheckNpmResourceHolder(resourceInfoWithNoVersion);
    //        verifyMeasure(
    //                MEASURE_URL_CROSS_PACKAGE_SOURCE,
    //                LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION,
    //                resourceInfoWithNoVersion);
    //        final NpmResourceHolder resourceInfoWithVersion =
    //                loader.loadNpmResources(new CanonicalType(MEASURE_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION));
    //        sanityCheckNpmResourceHolder(resourceInfoWithVersion);
    //        verifyMeasure(
    //                MEASURE_URL_CROSS_PACKAGE_SOURCE,
    //                LIBRARY_URL_CROSS_PACKAGE_SOURCE_WITH_VERSION,
    //                resourceInfoWithVersion);
    //
    //        verifyLibrary(
    //                LIBRARY_URL_CROSS_PACKAGE_SOURCE,
    //                expectedCqlSource,
    //                resourceInfoWithVersion.getOptMainLibrary().orElse(null));
    //
    //        final Optional<ILibraryAdapter> matchingLibraryWithSourceResult =
    //                resourceInfoWithVersion.findMatchingLibrary(new
    // VersionedIdentifier().withId(CROSS_PACKAGE_TARGET_ID));
    //
    //        // We expect NOT to find the target Library here since it's not in the source package at all
    //        assertTrue(matchingLibraryWithSourceResult.isEmpty());
    //
    //        // On the other hand, we can load the Library directly from the loader by URL:
    //        final Optional<ILibraryAdapter> optLibraryTarget =
    // loader.loadLibraryByUrl(LIBRARY_URL_CROSS_PACKAGE_TARGET);
    //
    //        assertTrue(optLibraryTarget.isPresent());
    //        verifyLibrary(LIBRARY_URL_CROSS_PACKAGE_TARGET, expectedCqlTarget, optLibraryTarget.get());
    //    }

    private void verifyLibrary(String expectedLibraryUrl, String expectedCql, @Nullable ILibraryAdapter library) {
        assertNotNull(library);

        assertEquals(
                getExpectedFhirVersion(), library.fhirContext().getVersion().getVersion());

        assertEquals(expectedLibraryUrl, library.getUrl());

        final List<ICompositeType> attachments = library.getContent();

        assertEquals(1, attachments.size());

        final ICompositeType attachment = attachments.get(0);

        final IAdapterFactory adapterFactory = IAdapterFactory.forFhirVersion(
                library.fhirContext().getVersion().getVersion());

        final IAttachmentAdapter adaptedAttachment = adapterFactory.createAttachment(attachment);

        assertEquals("text/cql", adaptedAttachment.getContentType());
        final byte[] attachmentData = adaptedAttachment.getData();
        final String cql = new String(attachmentData, StandardCharsets.UTF_8);

        assertEquals(expectedCql, cql);
    }
    //
    //    protected void verifyMeasure(String measureUrl, String expectedLibraryUrl, Optional<IMeasureAdapter>
    // measureAdapter) {
    //
    //        final Optional<IMeasureAdapter> optMeasure = npmResourceHolder.getMeasure();
    //        assertTrue(optMeasure.isPresent(), "Could not find measure with url: %s".formatted(measureUrl));
    //
    //        final IMeasureAdapter measure = optMeasure.get();
    //        assertEquals(
    //                getExpectedFhirVersion(), measure.fhirContext().getVersion().getVersion());
    //        assertEquals(measureUrl, measure.getUrl());
    //
    //        final List<String> libraryUrls = measure.getLibrary();
    //        assertEquals(1, libraryUrls.size());
    //        final String libraryUrl = libraryUrls.get(0);
    //        assertEquals(expectedLibraryUrl, libraryUrl);
    //    }
    //
    //    protected void verifyMeasure(String measureUrl, String expectedLibraryUrl, Optional<IMeasureAdapter>
    // measureAdapter) {
    //
    //        final Optional<IMeasureAdapter> optMeasure = npmResourceHolder.getMeasure();
    //        assertTrue(optMeasure.isPresent(), "Could not find measure with url: %s".formatted(measureUrl));
    //
    //        final IMeasureAdapter measure = optMeasure.get();
    //        assertEquals(
    //            getExpectedFhirVersion(), measure.fhirContext().getVersion().getVersion());
    //        assertEquals(measureUrl, measure.getUrl());
    //
    //        final List<String> libraryUrls = measure.getLibrary();
    //        assertEquals(1, libraryUrls.size());
    //        final String libraryUrl = libraryUrls.get(0);
    //        assertEquals(expectedLibraryUrl, libraryUrl);
    //    }

    @Nonnull
    protected R4NpmPackageLoaderInMemory setup(Path... tgzPaths) {
        return R4NpmPackageLoaderInMemory.fromNpmPackageClasspath(getClass(), tgzPaths);
    }
}
