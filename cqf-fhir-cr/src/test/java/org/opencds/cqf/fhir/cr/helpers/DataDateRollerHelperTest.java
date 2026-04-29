package org.opencds.cqf.fhir.cr.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.Test;

class DataDateRollerHelperTest {

    private static final FhirContext FHIR_CONTEXT_R4 = FhirContext.forR4Cached();

    @Test
    void rollsMedicationRequestAuthoredOnByOffset() {
        var anchor = LocalDate.of(2023, 4, 28);
        var today = LocalDate.of(2030, 1, 1);
        var originalAuthoredOn = LocalDate.of(2023, 4, 28);

        var medicationRequest = newMedicationRequestWithRoller(anchor, originalAuthoredOn, null, null);

        DataDateRollerHelper.rollIfAnnotated(medicationRequest, today, FHIR_CONTEXT_R4);

        assertEquals(today, dateOf(medicationRequest.getAuthoredOnElement()));
    }

    @Test
    void rollsValidityPeriodStartAndEnd() {
        var anchor = LocalDate.of(2023, 4, 28);
        var today = LocalDate.of(2030, 1, 1);

        var medicationRequest = newMedicationRequestWithRoller(
                anchor, LocalDate.of(2023, 4, 28), LocalDate.of(2023, 4, 28), LocalDate.of(2023, 7, 28));

        DataDateRollerHelper.rollIfAnnotated(medicationRequest, today, FHIR_CONTEXT_R4);

        var validityPeriod = medicationRequest.getDispenseRequest().getValidityPeriod();
        assertEquals(today, dateOf(validityPeriod.getStartElement()));
        // end was 91 days after anchor; should land 91 days after today.
        assertEquals(today.plusDays(91), dateOf(validityPeriod.getEndElement()));
    }

    @Test
    void stripsRollerExtensionAfterRollingForIdempotency() {
        var anchor = LocalDate.of(2023, 4, 28);
        var medicationRequest = newMedicationRequestWithRoller(anchor, LocalDate.of(2024, 4, 28), null, null);

        DataDateRollerHelper.rollIfAnnotated(medicationRequest, LocalDate.of(2030, 1, 1), FHIR_CONTEXT_R4);

        var hasRollerExt = medicationRequest.getExtension().stream()
                .anyMatch(e -> DataDateRollerHelper.EXT_URL.equals(e.getUrl()));
        assertFalse(hasRollerExt, "dataDateRoller extension should be stripped after rolling");
    }

    @Test
    void noOpForResourceWithoutRollerExtension() {
        var medicationRequest = new MedicationRequest();
        medicationRequest.setId("MedicationRequest/no-roller");
        medicationRequest.setAuthoredOnElement(new DateTimeType("2024-04-28"));

        DataDateRollerHelper.rollIfAnnotated(medicationRequest, LocalDate.of(2030, 1, 1), FHIR_CONTEXT_R4);

        assertEquals(LocalDate.of(2024, 4, 28), dateOf(medicationRequest.getAuthoredOnElement()));
    }

    @Test
    void timeTravelSanityKeepsAuthoredOnInTwoYearWindow() {
        // Walk a few years into the future and confirm the rolled authoredOn
        // never falls outside the 2-year window CQL expects.
        var anchor = LocalDate.of(2023, 4, 28);
        var originalAuthoredOn = LocalDate.of(2023, 4, 28);

        for (int yearOffset = 0; yearOffset <= 10; yearOffset++) {
            var today = LocalDate.of(2026 + yearOffset, 6, 15);
            var medicationRequest = newMedicationRequestWithRoller(anchor, originalAuthoredOn, null, null);

            DataDateRollerHelper.rollIfAnnotated(medicationRequest, today, FHIR_CONTEXT_R4);

            var rolled = dateOf(medicationRequest.getAuthoredOnElement());
            assertTrue(
                    !rolled.isBefore(today.minusYears(2)) && !rolled.isAfter(today),
                    "Rolled authoredOn (%s) must be within 2 years on or before today (%s)".formatted(rolled, today));
        }
    }

    private static MedicationRequest newMedicationRequestWithRoller(
            LocalDate anchor, LocalDate authoredOn, LocalDate validityStart, LocalDate validityEnd) {
        var medicationRequest = new MedicationRequest();
        medicationRequest.setId("MedicationRequest/test-rolling");
        medicationRequest.setAuthoredOnElement(new DateTimeType(authoredOn.toString()));
        if (validityStart != null || validityEnd != null) {
            var validityPeriod = new Period();
            if (validityStart != null) {
                validityPeriod.setStartElement(new DateTimeType(validityStart.toString()));
            }
            if (validityEnd != null) {
                validityPeriod.setEndElement(new DateTimeType(validityEnd.toString()));
            }
            medicationRequest.getDispenseRequest().setValidityPeriod(validityPeriod);
        }
        var rollerExtension = new Extension(DataDateRollerHelper.EXT_URL);
        rollerExtension.addExtension(
                new Extension(DataDateRollerHelper.EXT_DATE_LAST_UPDATED, new DateTimeType(anchor.toString())));
        medicationRequest.addExtension(rollerExtension);
        return medicationRequest;
    }

    private static LocalDate dateOf(DateTimeType primitive) {
        Date value = primitive.getValue();
        return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
