package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureReportConstants.EXT_SUPPORTING_EVIDENCE_URL;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.measure.common.SupportingEvidenceMode;
import org.opencds.cqf.fhir.cr.measure.r4.Measure.Given;

/**
 * Verifies that under {@link SupportingEvidenceMode#ALL_EXPRESSIONS} every evaluated expression
 * surfaces as supporting evidence from unmodified measures.
 */
class MeasureAllExpressionSupportingEvidenceTest {

    private static final Given given =
            Measure.given().repositoryFor("MeasureTest").supportingEvidenceMode(SupportingEvidenceMode.ALL_EXPRESSIONS);

    private static Given givenMode(SupportingEvidenceMode mode) {
        return Measure.given().repositoryFor("MeasureTest").supportingEvidenceMode(mode);
    }

    private static List<Extension> populationLevelEvidence(MeasureReport report) {
        return report.getGroupFirstRep().getPopulationFirstRep().getExtension().stream()
                .filter(e -> EXT_SUPPORTING_EVIDENCE_URL.equals(e.getUrl()))
                .toList();
    }

    private static List<Extension> reportLevelEvidence(MeasureReport report) {
        return report.getExtension().stream()
                .filter(e -> EXT_SUPPORTING_EVIDENCE_URL.equals(e.getUrl()))
                .toList();
    }

    private static List<String> evidenceNames(List<Extension> evidence) {
        return evidence.stream()
                .map(e -> e.getExtensionByUrl("name").getValue().primitiveValue())
                .toList();
    }

    private static Extension evidenceByName(MeasureReport report, String name) {
        return reportLevelEvidence(report).stream()
                .filter(e -> name.equals(e.getExtensionByUrl("name").getValue().primitiveValue()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No report-level evidence named '%s'. Present: %s"
                        .formatted(name, evidenceNames(reportLevelEvidence(report)))));
    }

    private static String valueOf(Extension evidence) {
        return evidence.getExtension().stream()
                .filter(e -> "value".equals(e.getUrl()))
                .map(e -> e.getValue() == null ? "(no value)" : e.getValue().primitiveValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * Verifies a measure that declares no supporting evidence still yields report-level evidence.
     */
    @Test
    void measureWithNoDeclaredEvidenceProducesReportLevelEvidence() {
        MeasureReport report = given.when()
                .measureId("CohortBooleanAllPopulations")
                .subject("patient-9")
                .evaluate()
                .then()
                .measureReport();

        var evidence = reportLevelEvidence(report);
        var names = evidenceNames(evidence);

        assertFalse(evidence.isEmpty(), "expected report-level evidence from an undeclared measure");

        // Expressions the measure never references, surfaced without touching its definition.
        assertTrue(names.contains("test tuple"), names::toString);
        assertTrue(names.contains("TestIntervalList"), names::toString);
        assertTrue(names.contains("Patient Age Bracket"), names::toString);

        // Resource-valued expressions surface as reference strings.
        assertTrue(names.contains("All Encounters"), names::toString);
        assertTrue(names.contains("PatientRes"), names::toString);
        assertEquals("Patient/patient-9", valueOf(evidenceByName(report, "PatientRes")));

        // Every name appears once, and each entry carries its name slice.
        assertEquals(names.size(), names.stream().distinct().count(), names::toString);
    }

    /**
     * Verifies declared evidence stays at population level and is not repeated at report level.
     */
    @Test
    void measureWithDeclaredSubsetKeepsItAtPopulationLevelAndAddsTheRest() {
        MeasureReport report = given.when()
                .measureId("CohortBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                .measureReport();

        var populationEvidence = populationLevelEvidence(report);

        // The authored entry is untouched: still at population level, still carrying its metadata.
        assertEquals(1, populationEvidence.size());
        assertEquals(List.of("DenominatorResource"), evidenceNames(populationEvidence));
        assertTrue(
                populationEvidence.get(0).getExtensionByUrl("description") != null,
                "declared entry should keep its authored description");

        var reportNames = evidenceNames(reportLevelEvidence(report));

        // The declared expression is not repeated at report level.
        assertFalse(reportNames.contains("Denominator Resource"), reportNames::toString);
        assertFalse(reportNames.contains("DenominatorResource"), reportNames::toString);

        // Undeclared expressions are surfaced alongside it.
        assertTrue(reportNames.contains("test tuple"), reportNames::toString);
        assertTrue(reportNames.contains("always true"), reportNames::toString);
    }

    /**
     * Verifies previously unencodable types encode at report level, and Time is valid FHIR.
     */
    @Test
    void previouslyUnencodableTypesAreEncodedAtReportLevel() {
        MeasureReport report = given.when()
                .measureId("SupportingEvidenceUndeclared")
                .subject("patient-9")
                .evaluate()
                .then()
                .measureReport();

        assertEquals("14:30:00", valueOf(evidenceByName(report, "Time Value")));
        assertEquals("08:00:00, 14:30:00", valueOf(evidenceByName(report, "List Time Value")));

        var quantity = evidenceByName(report, "Quantity Value")
                .getExtensionByUrl("value")
                .getValue();
        assertTrue(quantity instanceof org.hl7.fhir.r4.model.Quantity, () -> "was " + quantity.fhirType());
        assertEquals(
                "31.5", ((org.hl7.fhir.r4.model.Quantity) quantity).getValue().toPlainString());

        var ratio =
                evidenceByName(report, "Ratio Value").getExtensionByUrl("value").getValue();
        assertTrue(ratio instanceof org.hl7.fhir.r4.model.Ratio, () -> "was " + ratio.fhirType());

        var concept = evidenceByName(report, "Concept Value")
                .getExtensionByUrl("value")
                .getValue();
        assertTrue(concept instanceof org.hl7.fhir.r4.model.CodeableConcept, () -> "was " + concept.fhirType());

        var range = evidenceByName(report, "Interval Quantity Value")
                .getExtensionByUrl("value")
                .getValue();
        assertTrue(range instanceof org.hl7.fhir.r4.model.Range, () -> "was " + range.fhirType());

        // R4 has no integer64, so a Long renders as a string carrying the numeral.
        var longValue = evidenceByName(report, "Long Value").getExtensionByUrl("value");
        assertTrue(
                longValue.getValue() instanceof org.hl7.fhir.r4.model.StringType,
                () -> "was " + longValue.getValue().fhirType());
        assertEquals("31", longValue.getValue().primitiveValue());

        // ValueSet results render as canonical references, versioned when the library pins one.
        var valueSet = evidenceByName(report, "ValueSet Value").getExtensionByUrl("value");
        assertTrue(
                valueSet.getValue() instanceof org.hl7.fhir.r4.model.CanonicalType,
                () -> "was " + valueSet.getValue().fhirType());
        assertEquals(
                "http://example.org/ValueSet/example-conditions",
                valueSet.getValue().primitiveValue());
        assertEquals(
                "http://example.org/ValueSet/example-conditions-versioned|1.0",
                valueOf(evidenceByName(report, "Versioned ValueSet Value")));

        // The engine has no visitCodeSystemRef, so a CodeSystem-typed define evaluates to null
        // and surfaces as the data-absent marker instead of a canonical reference.
        var codeSystem = evidenceByName(report, "CodeSystem Value").getExtensionByUrl("value");
        assertTrue(codeSystem.getValue().hasExtension("http://hl7.org/fhir/StructureDefinition/data-absent-reason"));

        // Resource-valued expressions surface as reference strings.
        assertEquals("Patient/patient-9", valueOf(evidenceByName(report, "Resource List Value")));
        assertEquals("Patient/patient-9", valueOf(evidenceByName(report, "Single Resource Value")));
    }

    /**
     * Verifies the default DECLARED mode emits declared evidence only, as shipped.
     */
    @Test
    void declaredModeEmitsOnlyDeclaredEvidence() {
        MeasureReport report = givenMode(SupportingEvidenceMode.DECLARED)
                .when()
                .measureId("CohortBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                .measureReport();

        assertEquals(List.of("DenominatorResource"), evidenceNames(populationLevelEvidence(report)));
        assertTrue(reportLevelEvidence(report).isEmpty(), "DECLARED must not add report-level evidence");
    }

    /**
     * Verifies OFF suppresses declared evidence as well as report-level evidence.
     */
    @Test
    void offModeEmitsNoEvidenceAnywhere() {
        MeasureReport report = givenMode(SupportingEvidenceMode.OFF)
                .when()
                .measureId("CohortBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                .measureReport();

        assertTrue(populationLevelEvidence(report).isEmpty(), "OFF must suppress declared evidence");
        assertTrue(reportLevelEvidence(report).isEmpty(), "OFF must suppress report-level evidence");
    }

    /**
     * Verifies collection is visible on MeasureDef, independently of report building.
     */
    @Test
    void evaluatedExpressionsAreVisibleOnMeasureDef() {
        given.when()
                .measureId("CohortBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                .def()
                .hasNoErrors()
                // Collected: evaluated, in scope, not declared.
                .hasEvaluatedExpression("test tuple")
                .hasEvaluatedExpression("always true")
                // Resource-valued, collected as evidence alongside the rest.
                .hasEvaluatedExpression("All Encounters")
                // Declared on a population, so left to the population-level slice.
                .hasNoEvaluatedExpression("Denominator Resource");
    }

    /**
     * Verifies nothing is collected onto MeasureDef unless the mode enables it.
     */
    @Test
    void declaredModeCollectsNothingOntoMeasureDef() {
        givenMode(SupportingEvidenceMode.DECLARED)
                .when()
                .measureId("CohortBooleanSupportingEvidence")
                .subject("patient-9")
                .evaluate()
                .then()
                .def()
                .hasNoEvaluatedExpressions();
    }

    /**
     * Verifies summary reports carry no report-level evidence in any mode.
     */
    @Test
    void summaryReportsCarryNoEvidenceInAnyMode() {
        MeasureReport report = given.when()
                .measureId("CohortBooleanSupportingEvidence")
                .evaluate()
                .then()
                .measureReport();

        assertEquals(MeasureReport.MeasureReportType.SUMMARY, report.getType());
        assertTrue(reportLevelEvidence(report).isEmpty(), "summary reports must carry no evidence");
    }
}
