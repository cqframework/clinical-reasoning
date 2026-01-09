package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

public class MeasureSupportingEvidenceTest {

    private static final Given given = Measure.given().repositoryFor("MeasureTest");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
    ZoneId zone = ZoneId.systemDefault();

    ZonedDateTime mpStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, zone);
    ZonedDateTime mpEnd = ZonedDateTime.of(2024, 12, 31, 23, 59, 59, 0, zone);

    ZonedDateTime janStart = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, zone);
    ZonedDateTime janEnd = ZonedDateTime.of(2024, 1, 31, 23, 59, 59, 0, zone);

    ZonedDateTime febStart = ZonedDateTime.of(2024, 2, 1, 0, 0, 0, 0, zone);
    ZonedDateTime febEnd = ZonedDateTime.of(2024, 2, 29, 23, 59, 59, 0, zone);

    CodeableConcept expectedCodeableConcept = new CodeableConcept()
            .addCoding(new Coding()
                    .setSystem("http://example.org/fhir/CodeSystem/example-supporting-evidence-codes")
                    .setCode("denominator-resource")
                    .setDisplay("Denominator Resource"));

    /**
     * Verifies Ratio/Proportion MeasureScoring adds Evidence
     * Also overloads all currently covered CQL result type scenarios to ensure they show on Report as expected
     */
    @Test
    void ratioSupportingEvidenceAllCoveredTypes() {

        given.when()
                .measureId("RatioGroupBooleanAllPopulationsSuppEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .getExtDef("always true")
                .extensionDefHasResults()
                .up()
                .getExtDef("Denominator Resource")
                .extensionDefHasResults()
                .up()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore(1.0)
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .population("denominator")
                .hasCount(1)
                .getPopulationExtension("always true")
                .hasBooleanValue(true)
                .up()
                .getPopulationExtension("DenominatorResource")
                .hasName("DenominatorResource")
                .hasDescription("Example of supporting evidence that is a Denominator Resource")
                .hasCode(expectedCodeableConcept)
                .hasListResourceIdItem("Encounter/patient-9-encounter-2")
                .hasListResourceIdItem("Encounter/patient-9-encounter-1")
                .up()
                .getPopulationExtension("list of boolean")
                .hasListBooleanItem(true)
                .hasListBooleanItem(false)
                .up()
                .getPopulationExtension("string")
                .hasStringValue("string test")
                .up()
                .getPopulationExtension("date")
                .hasStringValue("2026-01-01")
                .up()
                .getPopulationExtension("decimal")
                .hasDecimalValue(31.31)
                .up()
                .getPopulationExtension("number")
                .hasIntegerValue(31)
                .up()
                .getPopulationExtension("list of dates")
                .hasListStringItem("2024-01-01")
                .hasListStringItem("2024-01-02")
                .hasListStringItem("2024-01-03")
                .up()
                .getPopulationExtension("list of numbers")
                .hasListIntegerItem(31)
                .hasListIntegerItem(88)
                .hasListIntegerItem(11)
                .up()
                .getPopulationExtension("list of string")
                .hasListStringItem("test1")
                .hasListStringItem("test2")
                .hasListStringItem("test3")
                .up()
                .getPopulationExtension("test tuple")
                .hasTupleInteger("number", 31)
                .hasTupleListStringItem("dates", "2024-01-01")
                .hasTupleListStringItem("dates", "2024-01-02")
                .hasTupleListStringItem("dates", "2024-01-03")
                .hasTupleString("birthYear", "string test")
                .up()
                .getPopulationExtension("interval")
                .hasPeriodValue(mpStart, mpEnd)
                .up()
                .getPopulationExtension("TestIntervalList")
                .hasListPeriodItem(janStart, janEnd)
                .hasListPeriodItem(febStart, febEnd)
                .up()
                .getPopulationExtension("TestDecimalList")
                .hasListDecimalItem(1.5)
                .hasListDecimalItem(2.75)
                .hasListDecimalItem(0.33333)
                .up()
                .getPopulationExtension("test code")
                .hasStringValue(
                        "Code { code: M, system: http://hl7.org/fhir/v3/AdministrativeGender, version: null, display: Male }")
                .up()
                .getPopulationExtension("EmptyListExample")
                .hasEmptyListResult()
                .up()
                .getPopulationExtension("NullExample")
                .hasNullResult() // since subjectResources is Set<Object> it appears as emptyList instead of Null
                .up()
                .getPopulationExtension("PatientRes")
                .hasResourceIdValue("Patient/patient-9")
                .up()
                .getPopulationExtension("list test tuple")
                .up()
                .up()
                .population("denominator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator-exclusion")
                .hasCount(0)
                .up()
                .population("numerator")
                .hasCount(1)
                .up()
                .hasScore("1.0")
                .up()
                .report();
    }

    /**
     * Verifies Summary Reports do not store or have supporting Evidence objects on Def or Report
     */
    @Test
    void ratioSummarySupportingEvidence() {

        given.when()
                .measureId("RatioGroupBooleanAllPopulationsSuppEvidence")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population("denominator")
                .assertNoSupportingEvidenceResults()
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .firstGroup()
                .population("denominator")
                .assertNoSupportingEvidence()
                .up()
                .up()
                .report();
    }

    /**
     * Verifies Cohort Scoring applies Supporting Evidence Results per Measure Definition
     */
    @Test
    void cohortSupportingEvidence() {

        given.when()
                .measureId("CohortBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .getExtDef("Denominator Resource")
                .extensionDefHasResults()
                .up()
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .getPopulationExtension("DenominatorResource")
                .hasName("DenominatorResource")
                .hasDescription("Example of supporting evidence that is a Denominator Resource")
                .hasCode(expectedCodeableConcept)
                .hasListResourceIdItem("Encounter/patient-9-encounter-2")
                .hasListResourceIdItem("Encounter/patient-9-encounter-1")
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Verifies Cont-Variable Scoring applies Supporting Evidence Results per Measure Definition
     */
    @Test
    void continuousVariableSupportingEvidence() {

        given.when()
                .measureId("ContinuousVariableBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasNoErrors()
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .getExtDef("Denominator Resource")
                .extensionDefHasResults()
                .up()
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .getPopulationExtension("DenominatorResource")
                .hasName("DenominatorResource")
                .hasDescription("Example of supporting evidence that is a Denominator Resource")
                .hasCode(expectedCodeableConcept)
                .hasListResourceIdItem("Encounter/patient-9-encounter-2")
                .hasListResourceIdItem("Encounter/patient-9-encounter-1")
                .up()
                .up()
                .up()
                .report();
    }

    /**
     * Verifies scenario when CQL expression reference is invalid or doesn't exist
     * Exception for subjectId: Patient/patient-9, Message: Supporting Evidence defined expression: 'Non Existent Expression', is not found in Evaluation Results
     */
    @Test
    void cohortSupportingEvidenceMissingExpression() {
        String errorMsg =
                "Exception for subjectId: Patient/patient-9, Message: Supporting Evidence defined expression: 'Non Existent Expression', is not found in Evaluation Results";
        given.when()
                .measureId("CohortBooleanSupportingEvidenceMissExp")
                .subject("patient-9")
                .evaluate()
                .then()
                // MeasureDef assertions (pre-scoring) - verify internal state after processing
                .def()
                .hasError(errorMsg)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .up()
                .up()
                // MeasureReport assertions (post-scoring) - verify FHIR resource output
                .report()
                .hasContainedOperationOutcomeMsg(errorMsg)
                .hasStatus(MeasureReportStatus.ERROR)
                .firstGroup()
                .population("initial-population")
                .hasCount(1)
                .up()
                .up()
                .report();
    }

    /**
     * Verifies scenario when CQL expression reference is not populated
     */
    @Test
    void cohortSupportingEvidenceNoExpression() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> {
            given.when()
                    .measureId("CohortBooleanSupportingEvidenceNoExp")
                    .subject("patient-9")
                    .evaluate()
                    .then()
                    .report();
        });

        assertTrue(ex.getMessage().contains("expression must not be null"), "Wrong error message: " + ex.getMessage());
    }

    /**
     * Verifies scenario when CQL name reference is missing
     */
    @Test
    void cohortSupportingEvidenceNoName() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> {
            given.when()
                    .measureId("CohortBooleanSupportingEvidenceNoName")
                    .subject("patient-9")
                    .evaluate()
                    .then()
                    .report();
        });

        assertTrue(ex.getMessage().contains("name must not be null"), "Wrong error message: " + ex.getMessage());
    }

    /**
     * Verifies scenario when defined Ext is missing required valueExpression
     */
    @Test
    void cohortSupportingEvidenceBadExt() {
        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () -> {
            given.when()
                    .measureId("CohortBooleanSupportingEvidenceBadExt")
                    .subject("patient-9")
                    .evaluate()
                    .then()
                    .report();
        });

        assertTrue(
                ex.getMessage().contains("Extension does not contain valueExpression"),
                "Wrong error message: " + ex.getMessage());
    }
}
