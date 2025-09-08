package org.opencds.cqf.fhir.cr.measure.r4.npm;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
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

class R4RepositoryOrNpmResourceProviderWithNpmTest extends BaseR4RepositoryOrNpmResourceProviderTest {

    private static final Given GIVEN_REPO = MultiMeasure.given().repositoryPlusNpmFor("BasicNpmPackages");

    @Override
    IgRepository getIgRepository() {
        return GIVEN_REPO.getIgRepository();
    }

    @Override
    CanonicalType getMeasureUrl1() {
        return new CanonicalType("http://example.com/Measure/SimpleAlpha");
    }

    @Override
    CanonicalType getMeasureUrl2() {
        return new CanonicalType("http://example.com/Measure/SimpleBravo");
    }

    @Override
    CanonicalType getMeasureUrl3() {
        return new CanonicalType("http://with-derived-library.npm.opencds.org/Measure/WithDerivedLibrary");
    }

    @Override
    IdType getMeasureId1() {
        return new IdType(ResourceType.Measure.name(), "SimpleAlpha");
    }

    @Override
    IdType getMeasureId2() {
        return new IdType(ResourceType.Measure.name(), "SimpleBravo");
    }

    @Override
    IdType getMeasureId3() {
        return new IdType(ResourceType.Measure.name(), "WithDerivedLibrary");
    }

    @Test
    @Override
    void foldMeasuresIds() {
        var measureEithers = Stream.of(getMeasureId1(), getMeasureId2(), getMeasureId3())
                .map(Eithers::<CanonicalType, IdType, Measure>forMiddle3)
                .toList();

        assertThrows(InvalidRequestException.class, () -> testSubject.foldMeasures(measureEithers));
    }

    @Test
    @Override
    void foldSingleMeasureEitherId() {
        var measureEither = getMeasureEitherForFoldMeasuresId();
        assertThrows(InvalidRequestException.class, () -> testSubject.foldMeasure(measureEither));
    }

    @Test
    @Override
    void foldWithCustomIdTypeHandlerMeasureId() {
        var measureEither = getMeasureEitherForFoldMeasuresId();
        var customIdTypeHandler = getCustomIdTypeHandler();
        assertThrows(
                InvalidRequestException.class,
                () -> testSubject.foldWithCustomIdTypeHandler(measureEither, customIdTypeHandler));
    }

    @Override
    void assertRepositoryOrNpm(MeasureOrNpmResourceHolder holder) {
        assertTrue(holder.isNpm());
    }
}
