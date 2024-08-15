package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.monad.Either;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@ExtendWith(MockitoExtension.class)
class R4MeasureServiceUtilsTest {
    private static final String PRACTITIONER = "Practitioner/pra1";
    private static final String PRACTITIONER_ROLE = "PractitionerRole/praRol1";
    private static final String ORGANIZATION = "Organization/org1";
    private static final String LOCATION = "Location/loc1";
    private static final String PATIENT = "Patient/pat1";
    private static final String GROUP = "Group/grp1";
    private static final IllegalArgumentException EXCEPTION_ILLEGAL_REPORTER = new IllegalArgumentException("R4MultiMeasureService requires '[ResourceType]/[ResourceId]' format to set MeasureReport.reporter reference.");
    private static final Either<Object, IllegalArgumentException> EITHER_ILLEGAL_ARGUMENT_EXCEPTION = Eithers.forRight(EXCEPTION_ILLEGAL_REPORTER);
    private static final Either<Optional<Reference>, Exception> EITHER_EMPTY_RESULT = buildEitherLeft(null);

    @Mock
    private Repository repository;

    private static Stream<Arguments> params() {
        return Stream.of(
            Arguments.of(null, EITHER_EMPTY_RESULT),
            Arguments.of(PRACTITIONER, buildEitherLeft(PRACTITIONER)),
            Arguments.of(PRACTITIONER_ROLE, buildEitherLeft(PRACTITIONER_ROLE)),
            Arguments.of(ORGANIZATION, buildEitherLeft(ORGANIZATION)),
            Arguments.of(LOCATION, buildEitherLeft(LOCATION)),
            Arguments.of(PATIENT, EITHER_ILLEGAL_ARGUMENT_EXCEPTION),
            Arguments.of(GROUP, EITHER_ILLEGAL_ARGUMENT_EXCEPTION)
        );
    }

    @ParameterizedTest
    @MethodSource("params")
    void getReporter(
            @Nullable String theReporter,
            Either<Optional<Reference>, Exception> theExpectedReporterOrError) {
        final R4MeasureServiceUtils subject = new R4MeasureServiceUtils(repository);

        if (theExpectedReporterOrError.isRight()) {
            final Exception expectedException = theExpectedReporterOrError.right();
            assertThrows(expectedException.getClass(), () -> subject.getReporter(theReporter), expectedException.getMessage());
        } else if (theExpectedReporterOrError.isLeft()) {
            final Optional<Reference> optReporter = subject.getReporter(theReporter);
            assertEquals(theExpectedReporterOrError.left().map(Reference::getReference), optReporter.map(Reference::getReference));
        } else {
            fail("Expecting an Either with only a left or a right but it has neither.");
        }
    }

    private static Either<Optional<Reference>, Exception> buildEitherLeft(@Nullable String theId) {
        return Eithers.forLeft(Optional.ofNullable(theId).map(Reference::new));
    }
}
