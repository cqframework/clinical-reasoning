package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class R4RepositoryOrNpmResourceProviderNonNpmTest extends BaseR4RepositoryOrNpmResourceProviderTest {

    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryFor("MinimalMeasureEvaluation");

    @Override
    IgRepository getIgRepository() {
        return GIVEN_REPO.getIgRepository();
    }

    @Override
    CanonicalType getMeasureUrl() {
        return new CanonicalType("http://example.com/Measure/MinimalProportionNoBasisSingleGroup");
    }

    @Override
    IdType getMeasureId() {
        return new IdType(ResourceType.Measure.name(), "MinimalProportionNoBasisSingleGroup");
    }

    @Test
    @Override
    void foldMeasuresEitherId() {
        var measureOrNpmResourceHolder = testSubject.foldMeasure(getMeasureEitherForFoldMeasuresId());

        assertNotNull(measureOrNpmResourceHolder);
    }

    @Test
    @Override
    void foldWithCustomIdTypeHandlerMeasureId() {
        var measureId = getMeasureId();
        var measureOrNpmResourceHolder =
                testSubject.foldWithCustomIdTypeHandler(getMeasureEitherForFoldMeasuresId(), getCustomIdTypeHandler());

        assertNotNull(measureOrNpmResourceHolder);
        var measure = measureOrNpmResourceHolder.getMeasure();
        assertNotNull(measure);
        assertEquals(measureId.asStringValue(), measure.getIdElement().asStringValue());
    }
}
