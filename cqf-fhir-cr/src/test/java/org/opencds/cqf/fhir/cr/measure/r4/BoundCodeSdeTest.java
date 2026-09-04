package org.opencds.cqf.fhir.cr.measure.r4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opencds.cqf.fhir.cr.measure.constant.MeasureConstants.EXT_SDE_REFERENCE_URL;

import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;
import org.hl7.fhir.r4.model.MedicationDispense;
import org.hl7.fhir.r4.model.MedicationDispense.MedicationDispenseStatus;
import org.junit.jupiter.api.Test;

/**
 * Integration coverage for bound-code elements on a resource-valued supplemental data element, e.g.
 * {@code MedicationDispense.status}.
 * <p>
 * HAPI declares a bound code as {@code Enumeration<T>}, but the CQL value carries only the code text.
 * A converter that derives the target HAPI type from the CQL value's own type name builds a
 * {@code CodeType} for it and then fails on assignment with
 * {@code IllegalArgumentException: Can not set org.hl7.fhir.r4.model.Enumeration field
 * org.hl7.fhir.r4.model.MedicationDispense.status to org.hl7.fhir.r4.model.CodeType}. Asking the HAPI
 * child definition for the type instead yields the {@code Enumeration} along with its
 * {@code EnumFactory}, which is what makes the code parse.
 *
 * @see org.opencds.cqf.fhir.cql.engine.parameters.CqlFhirParametersConverter
 */
@SuppressWarnings("squid:S2699")
class BoundCodeSdeTest {

    private static final Measure.Given GIVEN = Measure.given().repositoryFor("BoundCodeSde");

    private static final String SDE_ID = "sde-medication-dispense";

    @Test
    void boundCodeConvertsForSupplementalData() {
        var then = GIVEN.when()
                .measureId("BoundCodeSde")
                .subject("Patient/patient-dispense")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("subject")
                .evaluate()
                .then();

        then.hasStatus(MeasureReportStatus.COMPLETE)
                .hasExtension(EXT_SDE_REFERENCE_URL, 1)
                .extensionByValueReference("MedicationDispense/dispense-completed")
                .extensionHasSDEId(SDE_ID)
                .up()
                .report();

        var dispense = SdeValues.onlySupplementalDataResource(then.def().measureDef(), MedicationDispense.class);
        assertEquals("dispense-completed", dispense.getIdElement().getIdPart());
        // The bound code must round-trip as a parsed enumeration, not merely as text.
        assertEquals(MedicationDispenseStatus.COMPLETED, dispense.getStatus());
        assertEquals("completed", dispense.getStatusElement().getValueAsString());
        assertEquals(
                "1049502",
                dispense.getMedicationCodeableConcept().getCodingFirstRep().getCode());
    }

    /**
     * Population reports run the same accumulation and rendering over every subject.
     */
    @Test
    void boundCodeConvertsForSupplementalDataInPopulationReport() {
        GIVEN.when()
                .measureId("BoundCodeSde")
                .periodStart("2024-01-01")
                .periodEnd("2024-12-31")
                .reportType("population")
                .evaluate()
                .then()
                .hasStatus(MeasureReportStatus.COMPLETE)
                .hasExtension(EXT_SDE_REFERENCE_URL, 1)
                .extensionByValueReference("MedicationDispense/dispense-completed")
                .extensionHasSDEId(SDE_ID)
                .up()
                .report();
    }
}
