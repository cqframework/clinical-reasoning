package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure;
import org.opencds.cqf.fhir.cr.measure.r4.MultiMeasure.Given;
import org.opencds.cqf.fhir.utility.repository.ig.IgRepository;

class R4RepositoryOrNpmResourceProviderWithNpmTest extends BaseR4RepositoryOrNpmResourceProviderTest {

    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryPlusNpmFor("BasicNpmPackages");

    @Override
    IgRepository getIgRepository() {
        return GIVEN_REPO.getIgRepository();
    }

    @Override
    CanonicalType getMeasureUrl() {
        return new CanonicalType("http://example.com/Measure/SimpleAlpha");
    }

    @Override
    IdType getMeasureId() {
        return new IdType(ResourceType.Measure.name(), "SimpleAlpha");
    }

    @Test
    @Override
    void foldMeasuresEitherId() {
        assertThrows(InvalidRequestException.class, () -> testSubject.foldMeasure(getMeasureEitherForFoldMeasuresId()));
    }

    @Test
    @Override
    void foldWithCustomIdTypeHandlerMeasureId() {
        assertThrows(
                InvalidRequestException.class,
                () -> testSubject.foldWithCustomIdTypeHandler(
                        getMeasureEitherForFoldMeasuresId(), getCustomIdTypeHandler()));
    }
}
