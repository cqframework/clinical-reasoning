package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;
import org.opencds.cqf.fhir.utility.monad.Eithers;
import org.opencds.cqf.fhir.utility.npm.MeasureOrNpmResourceHolder;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class R4RepositoryOrNpmResourceProviderNonNpmTest extends BaseR4RepositoryOrNpmResourceProviderTest {

    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");

    @Override
    IgRepository getIgRepository() {
        return GIVEN_REPO.getIgRepository();
    }

    @Override
    CanonicalType getMeasureUrl1() {
        return new CanonicalType("http://example.com/Measure/MinimalProportionNoBasisSingleGroup");
    }

    @Override
    CanonicalType getMeasureUrl2() {
        return new CanonicalType("http://example.com/Measure/MinimalCohortBooleanBasisSingleGroup");
    }

    @Override
    CanonicalType getMeasureUrl3() {
        return new CanonicalType("http://example.com/Measure/MinimalProportionResourceBasisSingleGroup");
    }

    @Override
    IdType getMeasureId1() {
        return new IdType(ResourceType.Measure.name(), "MinimalProportionNoBasisSingleGroup");
    }

    @Override
    IdType getMeasureId2() {
        return new IdType(ResourceType.Measure.name(), "MinimalCohortBooleanBasisSingleGroup");
    }

    @Override
    IdType getMeasureId3() {
        return new IdType(ResourceType.Measure.name(), "MinimalProportionResourceBasisSingleGroup");
    }

    @Test
    @Override
    void foldSingleMeasureEitherId() {
        var measureOrNpmResourceHolder = testSubject.foldMeasure(getMeasureEitherForFoldMeasuresId());

        assertNotNull(measureOrNpmResourceHolder);
    }

    @Test
    @Override
    void foldMeasuresIds() {
        var measureEithers = Stream.of(getMeasureId1(), getMeasureId2(), getMeasureId3())
                .map(Eithers::<CanonicalType, IdType, Measure>forMiddle3)
                .toList();

        var holderList = testSubject.foldMeasures(measureEithers);
        assertNotNull(holderList);

        var measures = holderList.getMeasures();
        assertEquals(3, measures.size());

        var expectedMeasures = List.of(
                getMeasureHardCoded(getMeasureId1(), getMeasureUrl1()),
                getMeasureHardCoded(getMeasureId2(), getMeasureUrl2()),
                getMeasureHardCoded(getMeasureId3(), getMeasureUrl3()));

        assertMeasuresEquals(expectedMeasures, holderList.getMeasures());
    }

    @Test
    @Override
    void foldWithCustomIdTypeHandlerMeasureId() {
        var measureId = getMeasureId1();
        var measureOrNpmResourceHolder =
                testSubject.foldWithCustomIdTypeHandler(getMeasureEitherForFoldMeasuresId(), getCustomIdTypeHandler());

        assertNotNull(measureOrNpmResourceHolder);
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(measureId.asStringValue(), measure.getIdElement().asStringValue());
    }

    @Override
    void assertRepositoryOrNpm(MeasureOrNpmResourceHolder holder) {
        assertFalse(holder.isNpm());
    }
}
