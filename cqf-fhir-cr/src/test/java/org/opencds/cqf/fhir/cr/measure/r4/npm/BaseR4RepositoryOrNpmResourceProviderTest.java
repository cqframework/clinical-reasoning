package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Function;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

abstract class BaseR4RepositoryOrNpmResourceProviderTest {

    private static final String MEASURE_URL_HARD_CODED = "http://example.com/Measure/HardCoded";
    private static final IdType MEASURE_ID_HARD_CODED = new IdType(ResourceType.Measure.name(), "HardCoded");

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
        final CanonicalType url = getMeasureUrl();
        final Measure measure = testSubject.resolveByUrl(url);

        assertNotNull(measure);
        assertEquals(url.asStringValue(), measure.getUrl());
    }

    @Test
    void foldMeasuresEitherUrl() {
        var url = getMeasureUrl();
        var measureOrNpmResourceHolder = testSubject.foldMeasure(getMeasureEitherForFoldMeasuresUrl());

        assertNotNull(measureOrNpmResourceHolder);
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(url.asStringValue(), measure.getUrl());
    }

    @Test
    abstract void foldMeasuresEitherId();

    @Test
    void foldMeasuresEitherMeasureResource() {
        var measureOrNpmResourceHolder = testSubject.foldMeasure(getMeasureEitherForFoldMeasuresResource());

        assertNotNull(measureOrNpmResourceHolder);
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(MEASURE_ID_HARD_CODED, measure.getIdElement());
        assertEquals(MEASURE_URL_HARD_CODED, measure.getUrl());
    }

    @Test
    void foldWithCustomIdTypeHandlerMeasureUrl() {
        var url = getMeasureUrl();
        var measureOrNpmResourceHolder =
                testSubject.foldWithCustomIdTypeHandler(getMeasureEitherForFoldMeasuresUrl(), getCustomIdTypeHandler());

        assertNotNull(measureOrNpmResourceHolder);
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
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(MEASURE_ID_HARD_CODED, measure.getIdElement());
        assertEquals(MEASURE_URL_HARD_CODED, measure.getUrl());
    }

    private Either3<CanonicalType, IdType, Measure> getMeasureEitherForFoldMeasuresUrl() {
        return Eithers.forLeft3(getMeasureUrl());
    }

    Either3<CanonicalType, IdType, Measure> getMeasureEitherForFoldMeasuresId() {
        return Eithers.forMiddle3(getMeasureId());
    }

    private Either3<CanonicalType, IdType, Measure> getMeasureEitherForFoldMeasuresResource() {
        return Eithers.forRight3(getMeasure());
    }

    Function<? super IdType, Measure> getCustomIdTypeHandler() {
        return idType -> (Measure) new Measure().setId(idType);
    }

    private Measure getMeasure() {
        return (Measure) new Measure().setUrl(MEASURE_URL_HARD_CODED).setId(MEASURE_ID_HARD_CODED);
    }

    abstract CanonicalType getMeasureUrl();

    abstract IdType getMeasureId();
}
