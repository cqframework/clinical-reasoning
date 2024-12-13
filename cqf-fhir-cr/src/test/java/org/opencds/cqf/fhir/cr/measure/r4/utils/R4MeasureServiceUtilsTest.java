package org.opencds.cqf.fhir.cr.measure.r4.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.measure.common.MeasureEvalType;
import org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants;
import org.opencds.cqf.fhir.cr.measure.r4.R4MeasureEvalType;
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
    private Repository repository;

    private R4MeasureServiceUtils testSubject;

    @BeforeEach
    void beforeEach() {
        testSubject = new R4MeasureServiceUtils(repository);
    }

    private static Stream<Arguments> getReporterParams() {
        return Stream.of(
                Arguments.of(null, EITHER_EMPTY_RESULT),
                Arguments.of(PRACTITIONER, buildEitherLeft(PRACTITIONER)),
                Arguments.of(PRACTITIONER_ROLE, buildEitherLeft(PRACTITIONER_ROLE)),
                Arguments.of(ORGANIZATION, buildEitherLeft(ORGANIZATION)),
                Arguments.of(LOCATION, buildEitherLeft(LOCATION)),
                Arguments.of(PATIENT, EITHER_ILLEGAL_ARGUMENT_EXCEPTION),
                Arguments.of(GROUP, EITHER_ILLEGAL_ARGUMENT_EXCEPTION));
    }

    @ParameterizedTest
    @MethodSource("getReporterParams")
    void getReporter(@Nullable String theReporter, Either<Optional<Reference>, Exception> theExpectedReporterOrError) {

        if (theExpectedReporterOrError.isRight()) {
            final Exception expectedException = theExpectedReporterOrError.rightOrThrow();
            assertThrows(
                    expectedException.getClass(),
                    () -> testSubject.getReporter(theReporter),
                    expectedException.getMessage());
        } else if (theExpectedReporterOrError.isLeft()) {
            final Optional<Reference> optReporter = testSubject.getReporter(theReporter);
            assertEquals(
                    theExpectedReporterOrError.leftOrThrow().map(Reference::getReference),
                    optReporter.map(Reference::getReference));
        } else {
            fail("Expecting an Either with only a left or a right but it has neither.");
        }
    }

    private static Stream<Arguments> getMeasureEvalTypeHappyPathParams() {
        return Stream.of(
                Arguments.of(null, null, MeasureEvalType.POPULATION),
                Arguments.of(R4MeasureEvalType.SUBJECT.toCode(), null, MeasureEvalType.SUBJECT),
                Arguments.of(R4MeasureEvalType.SUBJECTLIST.toCode(), null, MeasureEvalType.SUBJECTLIST),
                Arguments.of(R4MeasureEvalType.POPULATION.toCode(), null, MeasureEvalType.POPULATION),
                Arguments.of(null, Collections.emptyList(), MeasureEvalType.POPULATION),
                Arguments.of(R4MeasureEvalType.SUBJECT.toCode(), Collections.emptyList(), MeasureEvalType.SUBJECT),
                Arguments.of(
                        R4MeasureEvalType.SUBJECTLIST.toCode(), Collections.emptyList(), MeasureEvalType.SUBJECTLIST),
                Arguments.of(
                        R4MeasureEvalType.POPULATION.toCode(), Collections.emptyList(), MeasureEvalType.POPULATION),
                Arguments.of(null, Collections.singletonList(null), MeasureEvalType.POPULATION),
                Arguments.of(
                        R4MeasureEvalType.SUBJECT.toCode(), Collections.singletonList(null), MeasureEvalType.SUBJECT),
                Arguments.of(
                        R4MeasureEvalType.SUBJECTLIST.toCode(),
                        Collections.singletonList(null),
                        MeasureEvalType.SUBJECTLIST),
                Arguments.of(
                        R4MeasureEvalType.POPULATION.toCode(),
                        Collections.singletonList(null),
                        MeasureEvalType.POPULATION),
                Arguments.of(null, Collections.singletonList(PATIENT_PAT_1), MeasureEvalType.SUBJECT),
                Arguments.of(
                        R4MeasureEvalType.SUBJECT.toCode(),
                        Collections.singletonList(PATIENT_PAT_1),
                        MeasureEvalType.SUBJECT),
                Arguments.of(
                        R4MeasureEvalType.SUBJECTLIST.toCode(),
                        Collections.singletonList(PATIENT_PAT_1),
                        MeasureEvalType.SUBJECTLIST),
                Arguments.of(
                        R4MeasureEvalType.POPULATION.toCode(),
                        Collections.singletonList(PATIENT_PAT_1),
                        MeasureEvalType.POPULATION));
    }

    @ParameterizedTest
    @MethodSource("getMeasureEvalTypeHappyPathParams")
    void getMeasureEvalTypeHappyPath(
            @Nullable String reportType, @Nullable List<String> subjectIds, MeasureEvalType expectedMeasureEvalType) {
        assertThat(testSubject.getMeasureEvalType(reportType, subjectIds), equalTo(expectedMeasureEvalType));
    }

    private static Stream<Arguments> getMeasureEvalTypeSingleSubjectHappyPathParams() {
        return Stream.of(
                Arguments.of(null, null, MeasureEvalType.POPULATION),
                Arguments.of(R4MeasureEvalType.SUBJECT.toCode(), null, MeasureEvalType.SUBJECT),
                Arguments.of(R4MeasureEvalType.SUBJECTLIST.toCode(), null, MeasureEvalType.SUBJECTLIST),
                Arguments.of(R4MeasureEvalType.POPULATION.toCode(), null, MeasureEvalType.POPULATION),
                Arguments.of(null, PATIENT_PAT_1, MeasureEvalType.SUBJECT),
                Arguments.of(MeasureEvalType.SUBJECT.toCode(), PATIENT_PAT_1, MeasureEvalType.SUBJECT),
                Arguments.of(MeasureEvalType.SUBJECTLIST.toCode(), PATIENT_PAT_1, MeasureEvalType.SUBJECTLIST),
                Arguments.of(MeasureEvalType.POPULATION.toCode(), PATIENT_PAT_1, MeasureEvalType.POPULATION));
    }

    @ParameterizedTest
    @MethodSource("getMeasureEvalTypeSingleSubjectHappyPathParams")
    void getMeasureEvalTypeSingleSubjectHappyPath(
            @Nullable String reportType, @Nullable String subjectId, MeasureEvalType expectedMeasureEvalType) {
        assertThat(testSubject.getMeasureEvalType(reportType, subjectId), equalTo(expectedMeasureEvalType));
    }

    private static Stream<Arguments> getMeasureEvalTypeErrorsParams() {
        return Stream.of(
                Arguments.of(
                        MeasureEvalType.PATIENT.toCode(),
                        null,
                        new Exception("ReportType: patient, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENTLIST.toCode(),
                        null,
                        new Exception("ReportType: patient-list, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENT.toCode(),
                        Collections.emptyList(),
                        new Exception("ReportType: patient, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENTLIST.toCode(),
                        Collections.emptyList(),
                        new Exception("ReportType: patient-list, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENT.toCode(),
                        Collections.singletonList(null),
                        new Exception("ReportType: patient, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENTLIST.toCode(),
                        Collections.singletonList(null),
                        new Exception("ReportType: patient-list, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENT.toCode(),
                        Collections.singletonList(PATIENT_PAT_1),
                        new Exception("ReportType: patient, is not an accepted R4 EvalType value.")),
                Arguments.of(
                        MeasureEvalType.PATIENTLIST.toCode(),
                        Collections.singletonList(PATIENT_PAT_1),
                        new Exception("ReportType: patient-list, is not an accepted R4 EvalType value.")));
    }

    @ParameterizedTest
    @MethodSource("getMeasureEvalTypeErrorsParams")
    void getMeasureEvalTypeErrors(
            @Nullable String reportType, @Nullable List<String> subjectIds, Exception expectedException) {
        var actualException =
                assertThrows(Exception.class, () -> testSubject.getMeasureEvalType(reportType, subjectIds));

        assertThat(actualException.getMessage(), containsString(expectedException.getMessage()));
    }

    private static Stream<Arguments> productLineParams() {
        return Stream.of(
                Arguments.of("Medicare", buildExtensionForProductLine("Medicare")),
                Arguments.of("Medicaid", buildExtensionForProductLine("Medicaid")),
                Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("productLineParams")
    void productLine(@Nullable String productLine, @Nullable Extension expectedExtension) {
        var measureReportWithProductLine = testSubject.addProductLineExtension(new MeasureReport(), productLine);

        var actualExtension = measureReportWithProductLine.getExtensionByUrl(
                MeasureReportConstants.MEASUREREPORT_PRODUCT_LINE_EXT_URL);

        if (expectedExtension == null) {
            assertNull(actualExtension);
            return;
        }

        assertThat(actualExtension.getUrl(), equalTo(expectedExtension.getUrl()));

        assertThat(
                actualExtension.getValue().primitiveValue(),
                equalTo(expectedExtension.getValue().primitiveValue()));
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
