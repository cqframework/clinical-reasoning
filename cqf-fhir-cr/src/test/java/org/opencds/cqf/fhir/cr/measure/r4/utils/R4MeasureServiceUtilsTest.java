package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;
import org.opencds.cqf.fhir.utility.monad.Either;
import org.opencds.cqf.fhir.utility.monad.Eithers;

@ExtendWith(MockitoExtension.class)
class R4MeasureServiceUtilsTest {
    private static final String PRACTITIONER = "Practitioner/pra1";
    private static final String PRACTITIONER_ROLE = "PractitionerRole/praRol1";
    private static final String ORGANIZATION = "Organization/org1";
    private static final String LOCATION = "Location/loc1";
    private static final String PATIENT_PAT_1 = "Patient/pat1";
    private static final String PATIENT = PATIENT_PAT_1;
    private static final String GROUP = "Group/grp1";
    private static final IllegalArgumentException EXCEPTION_ILLEGAL_REPORTER = new IllegalArgumentException(
            "R4MultiMeasureService requires '[ResourceType]/[ResourceId]' format to set MeasureReport.reporter reference.");
    private static final Either<Object, IllegalArgumentException> EITHER_ILLEGAL_ARGUMENT_EXCEPTION =
            Eithers.forRight(EXCEPTION_ILLEGAL_REPORTER);
    private static final Either<Optional<Reference>, Exception> EITHER_EMPTY_RESULT = buildEitherLeft(null);

    @Mock
    private IRepository repository;

    private R4MeasureServiceUtils testSubject;

    @BeforeEach
    void beforeEach() {
        testSubject = new R4MeasureServiceUtils(repository);
    }

    private record GetReporterParams(
            @Nullable String theReporter, Either<Optional<Reference>, Exception> theExpectedReporterOrError) {}

    private static Stream<GetReporterParams> getReporterParams() {
        return Stream.of(
                new GetReporterParams(null, EITHER_EMPTY_RESULT),
                new GetReporterParams(PRACTITIONER, buildEitherLeft(PRACTITIONER)),
                new GetReporterParams(PRACTITIONER_ROLE, buildEitherLeft(PRACTITIONER_ROLE)),
                new GetReporterParams(ORGANIZATION, buildEitherLeft(ORGANIZATION)),
                new GetReporterParams(LOCATION, buildEitherLeft(LOCATION)),
                new GetReporterParams(PATIENT, Eithers.forRight(EXCEPTION_ILLEGAL_REPORTER)),
                new GetReporterParams(GROUP, Eithers.forRight(EXCEPTION_ILLEGAL_REPORTER)));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("getReporterParams")
    void getReporter(GetReporterParams testCase) {

        if (testCase.theExpectedReporterOrError().isRight()) {
            final Exception expectedException =
                    testCase.theExpectedReporterOrError().rightOrThrow();
            assertThrows(
                    expectedException.getClass(),
                    () -> testSubject.getReporter(testCase.theReporter()),
                    expectedException.getMessage());
        } else if (testCase.theExpectedReporterOrError().isLeft()) {
            final Optional<Reference> optReporter = testSubject.getReporter(testCase.theReporter());
            assertEquals(
                    testCase.theExpectedReporterOrError().leftOrThrow().map(Reference::getReference),
                    optReporter.map(Reference::getReference));
        } else {
            fail("Expecting an Either with only a left or a right but it has neither.");
        }
    }

    private record ProductLineParams(
            @Nullable String productLine, @Nullable Extension expectedExtension) {}

    private static Stream<ProductLineParams> productLineParams() {
        return Stream.of(
                new ProductLineParams("Medicare", buildExtensionForProductLine("Medicare")),
                new ProductLineParams("Medicaid", buildExtensionForProductLine("Medicaid")),
                new ProductLineParams(null, null));
    }

    @ParameterizedTest(name = "{index} => testCase={0}")
    @MethodSource("productLineParams")
    void productLine(ProductLineParams testCase) {
        var measureReportWithProductLine =
                testSubject.addProductLineExtension(new MeasureReport(), testCase.productLine());

        var actualExtension = measureReportWithProductLine.getExtensionByUrl(
                MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL);

        if (testCase.expectedExtension() == null) {
            assertNull(actualExtension);
            return;
        }

        assertThat(
                actualExtension.getUrl(), equalTo(testCase.expectedExtension().getUrl()));

        assertThat(
                actualExtension.getValue().primitiveValue(),
                equalTo(testCase.expectedExtension().getValue().primitiveValue()));
    }

    private static Extension buildExtensionForProductLine(String productLine) {
        return new Extension()
                .setUrl(MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL)
                .setValue(new StringType(productLine));
    }

    private static Either<Optional<Reference>, Exception> buildEitherLeft(@Nullable String theId) {
        return Eithers.forLeft(Optional.ofNullable(theId).map(Reference::new));
    }
}
