package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolder;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolderList;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

abstract class BaseR4RepositoryOrNpmResourceProviderTest {

    private static final String MEASURE_URL_HARD_CODED_1 = "http://example.com/Measure/HardCoded1";
    private static final IdType MEASURE_ID_HARD_CODED_1 = new IdType(ResourceType.Measure.name(), "HardCoded1");
    private static final String MEASURE_URL_HARD_CODED_2 = "http://example.com/Measure/HardCoded2";
    private static final IdType MEASURE_ID_HARD_CODED_2 = new IdType(ResourceType.Measure.name(), "HardCoded2");
    private static final String MEASURE_URL_HARD_CODED_3 = "http://example.com/Measure/HardCoded3";
    private static final IdType MEASURE_ID_HARD_CODED_3 = new IdType(ResourceType.Measure.name(), "HardCoded3");

    private final EvaluationSettings evaluationSettings = EvaluationSettings.getDefault();

    abstract IgRepository getIgRepository();

    R4RepositoryOrNpmResourceProvider testSubject;

    @BeforeEach
    void beforeEach() {
        final IgRepository igRepository = getIgRepository();
        final NpmPackageLoader npmPackageLoader = igRepository.getNpmPackageLoader();
        evaluationSettings.setUseNpmForQualifyingResources(igRepository.hasNpm());
        testSubject = new R4RepositoryOrNpmResourceProvider(igRepository, npmPackageLoader, evaluationSettings);
    }

    @Test
    void resolveByUrl() {
        final CanonicalType url = getMeasureUrl1();
        final Measure measure = testSubject.resolveByUrl(url);

        assertNotNull(measure);
        assertEquals(url.asStringValue(), measure.getUrl());
    }

    @Test
    void foldSingleMeasureEitherUrl() {
        var url = getMeasureUrl1();
        var measureOrNpmResourceHolder = testSubject.foldMeasure(getMeasureEitherForFoldMeasuresUrl());

        assertNotNull(measureOrNpmResourceHolder);
        assertRepositoryOrNpm(measureOrNpmResourceHolder);
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(url.asStringValue(), measure.getUrl());
    }

    @Test
    abstract void foldSingleMeasureEitherId();

    @Test
    void foldSingleMeasureEitherMeasureResource() {
        var measureOrNpmResourceHolder = testSubject.foldMeasure(getMeasureEitherForFoldMeasuresResource());

        assertNotNull(measureOrNpmResourceHolder);
        assertFalse(measureOrNpmResourceHolder.isNpm());
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(MEASURE_ID_HARD_CODED_1, measure.getIdElement());
        assertEquals(MEASURE_URL_HARD_CODED_1, measure.getUrl());
    }

    @Test
    void foldMeasuresUrls() {
        var measureEithers = Stream.of(getMeasureUrl1(), getMeasureUrl2(), getMeasureUrl3())
                .map(Eithers::<CanonicalType, IdType, Measure>forLeft3)
                .toList();

        var holderList = testSubject.foldMeasures(measureEithers);
        assertNotNull(holderList);
        assertRepositoryOrNpm(holderList);

        var measures = holderList.getMeasures();
        assertEquals(3, measures.size());
        var expectedMeasures = List.of(
                getMeasureHardCoded(getMeasureId1(), getMeasureUrl1()),
                getMeasureHardCoded(getMeasureId2(), getMeasureUrl2()),
                getMeasureHardCoded(getMeasureId3(), getMeasureUrl3()));

        assertMeasuresEquals(expectedMeasures, holderList.getMeasures());
    }

    @Test
    abstract void foldMeasuresIds();

    @Test
    void foldMeasuresResources() {

        var measureEithers = Stream.of(getMeasureHardCoded1(), getMeasureHardCoded2(), getMeasureHardCoded3())
                .map(Eithers::<CanonicalType, IdType, Measure>forRight3)
                .toList();

        var holderList = testSubject.foldMeasures(measureEithers);
        assertNotNull(holderList);
        assertNonNpm(holderList);

        var measures = holderList.getMeasures();
        assertEquals(3, measures.size());
        assertMeasuresEquals(
                List.of(getMeasureHardCoded1(), getMeasureHardCoded2(), getMeasureHardCoded3()),
                holderList.getMeasures());
    }

    @Test
    void foldWithCustomIdTypeHandlerMeasureUrl() {
        var url = getMeasureUrl1();
        var measureOrNpmResourceHolder =
                testSubject.foldWithCustomIdTypeHandler(getMeasureEitherForFoldMeasuresUrl(), getCustomIdTypeHandler());

        assertNotNull(measureOrNpmResourceHolder);
        assertRepositoryOrNpm(measureOrNpmResourceHolder);
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(url.asStringValue(), measure.getUrl());
    }

    @Test
    abstract void foldWithCustomIdTypeHandlerMeasureId();

    @Test
    void foldWithCustomIdTypeHandlerMeasureResource() {
        var measureOrNpmResourceHolder = testSubject.foldWithCustomIdTypeHandler(
                getMeasureEitherForFoldMeasuresResource(), getCustomIdTypeHandler());

        assertNotNull(measureOrNpmResourceHolder);
        assertFalse(measureOrNpmResourceHolder.isNpm());

        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(MEASURE_ID_HARD_CODED_1, measure.getIdElement());
        assertEquals(MEASURE_URL_HARD_CODED_1, measure.getUrl());
    }

    @Test
    void getMeasureEithersInvalidBothPopulated() {
        var measureIds = List.of("x");
        var measureUrls = List.of("y");

        assertThrows(InvalidRequestException.class, () -> testSubject.getMeasureEithers(measureIds, measureUrls));
    }

    @Test
    void getMeasureEithersInvalidBothNull() {
        assertThrows(InvalidRequestException.class, () -> testSubject.getMeasureEithers(null, null));
    }

    @Test
    void getMeasureEithersInvalidBothEmpty() {
        assertThrows(InvalidRequestException.class, () -> testSubject.getMeasureEithers(List.of(), List.of()));
    }

    @Test
    void getMeasureEithersIds() {
        var actualMeasureEithers = testSubject.getMeasureEithers(List.of("x", "y", "z"), null);
        var expectedMeasureIds = Stream.of("x", "y", "z")
                .map(IdType::new)
                .map(Eithers::forMiddle3)
                .toList();

        assertNotNull(actualMeasureEithers);
        assertEquals(expectedMeasureIds, actualMeasureEithers);
    }

    @Test
    void getMeasureEithersUrls() {
        var actualMeasureEithers =
                testSubject.getMeasureEithers(List.of(), List.of("fakeUrl1", "fakeUrl2", "fakeUrl3"));
        var expectedMeasureEithers = Stream.of("fakeUrl1", "fakeUrl2", "fakeUrl3")
                .map(CanonicalType::new)
                .map(Eithers::<CanonicalType, IdType, Measure>forLeft3)
                .toList();

        assertNotNull(actualMeasureEithers);
        assertMeasureEithers(expectedMeasureEithers, actualMeasureEithers);
    }

    private Either3<CanonicalType, IdType, Measure> getMeasureEitherForFoldMeasuresUrl() {
        return Eithers.forLeft3(getMeasureUrl1());
    }

    Either3<CanonicalType, IdType, Measure> getMeasureEitherForFoldMeasuresId() {
        return Eithers.forMiddle3(getMeasureId1());
    }

    private Either3<CanonicalType, IdType, Measure> getMeasureEitherForFoldMeasuresResource() {
        return Eithers.forRight3(getMeasureHardCoded1());
    }

    Function<? super IdType, Measure> getCustomIdTypeHandler() {
        return idType -> (Measure) new Measure().setId(idType);
    }

    private Measure getMeasureHardCoded1() {
        return getMeasureHardCoded(MEASURE_ID_HARD_CODED_1, MEASURE_URL_HARD_CODED_1);
    }

    private Measure getMeasureHardCoded2() {
        return getMeasureHardCoded(MEASURE_ID_HARD_CODED_2, MEASURE_URL_HARD_CODED_2);
    }

    private Measure getMeasureHardCoded3() {
        return getMeasureHardCoded(MEASURE_ID_HARD_CODED_3, MEASURE_URL_HARD_CODED_3);
    }

    Measure getMeasureHardCoded(IdType id, CanonicalType url) {
        return (Measure) new Measure().setUrl(url.asStringValue()).setId(id);
    }

    private Measure getMeasureHardCoded(IdType id, String url) {
        return (Measure) new Measure().setUrl(url).setId(id);
    }

    private void assertMeasureEithers(
            List<Either3<CanonicalType, IdType, Measure>> expectedMeasureEithers,
            List<Either3<CanonicalType, IdType, Measure>> actualMeasureEithers) {

        assertEquals(expectedMeasureEithers.size(), actualMeasureEithers.size());

        for (int index = 0; index < expectedMeasureEithers.size(); index++) {
            var expectedEither = expectedMeasureEithers.get(0);
            var actualEither = actualMeasureEithers.get(0);

            assertEquals(expectedEither.isLeft(), actualEither.isLeft());
            assertEquals(expectedEither.isMiddle(), actualEither.isMiddle());
            assertEquals(expectedEither.isRight(), actualEither.isRight());

            if (expectedEither.isLeft()) {
                assertEquals(
                        expectedEither.leftOrThrow().getValueAsString(),
                        actualEither.leftOrThrow().getValueAsString());
            } else {
                assertEquals(expectedEither, actualEither);
            }
        }
    }

    void assertMeasuresEquals(List<Measure> expectedMeasures, List<Measure> actualMeasures) {

        assertEquals(expectedMeasures.size(), actualMeasures.size());

        for (int index = 0; index < expectedMeasures.size(); index++) {
            var expectedMeasure = expectedMeasures.get(index);
            var actualMeasure = actualMeasures.get(index);

            assertEquals(expectedMeasure.getId(), actualMeasure.getId());
            assertEquals(expectedMeasure.getUrl(), actualMeasure.getUrl());
        }
    }

    abstract CanonicalType getMeasureUrl1();

    abstract CanonicalType getMeasureUrl2();

    abstract CanonicalType getMeasureUrl3();

    abstract IdType getMeasureId1();

    abstract IdType getMeasureId2();

    abstract IdType getMeasureId3();

    void assertRepositoryOrNpm(MeasureOrNpmResourceHolderList holderList) {
        for (MeasureOrNpmResourceHolder measureOrNpmResourceHolder : holderList.getMeasuresOrNpmResourceHolders()) {
            assertRepositoryOrNpm(measureOrNpmResourceHolder);
        }
    }

    // In the case of Eithers with Measure resources, we always expect non-NPM, for now
    private void assertNonNpm(MeasureOrNpmResourceHolderList holderList) {
        for (MeasureOrNpmResourceHolder holder : holderList.measuresOrNpmResourceHolders()) {
            // If we're passing through resources, we always mark them as non-NPM for now
            assertFalse(holder.isNpm());
        }
    }

    abstract void assertRepositoryOrNpm(MeasureOrNpmResourceHolder holder);
}
