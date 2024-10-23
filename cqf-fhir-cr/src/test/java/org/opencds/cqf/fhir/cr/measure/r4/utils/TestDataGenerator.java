package org.opencds.cqf.fhir.cr.measure.r4.utils;

import java.util.Collections;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.opencds.cqf.fhir.api.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class was created in order to generate test data in memory for a variety of use cases.
 */
public class TestDataGenerator {
    private final Repository repository;
    private final Logger ourLog = LoggerFactory.getLogger(TestDataGenerator.class);

    public TestDataGenerator(Repository repository) {

        this.repository = repository;
    }

    public void makePatient(String practitioner, String organization, Period encounterPeriod) {
        int patientQty = 10;
        int i = 0;
        while (i < patientQty) {
            // Patient Creation
            var patientId = "Patient/patient-" + i;
            createPatient(patientId, practitioner, organization, i);
            // Encounter Creation

            if (i < 2) {
                // in-progress, den-exception
                createEncounter(patientId, encounterPeriod, EncounterStatus.INPROGRESS, i, 1);
            }
            if (i >= 2 && i < 4) {
                // arrived, Num exclusion
                createEncounter(patientId, encounterPeriod, EncounterStatus.ARRIVED, i, 1);
            }
            if (i >= 4 && i < 6) {
                // not-finished, boolean strat, otherwise numerator
                createEncounter(patientId, encounterPeriod, EncounterStatus.TRIAGED, i, 1);
            }
            if (i >= 6 && i < 8) {
                // cancelled, den exclusion
                createEncounter(patientId, encounterPeriod, EncounterStatus.CANCELLED, i, 1);
            }
            if (i >= 8) {
                // finished, numerator
                createEncounter(patientId, encounterPeriod, EncounterStatus.FINISHED, i, 1);
                if (i == 9) {
                    // resource based numerator will show 1 inNumerator, the other in denException
                    createEncounter(patientId, encounterPeriod, EncounterStatus.INPROGRESS, i, 2);
                }
            }
            // increment
            i++;
        }
        ourLog.info(String.format("Patients created: %s", i));
    }

    public void createPatient(String patientId, String practitionerId, String organization, int count) {
        // Patient Creation
        Patient patient = new Patient();
        patient.setId(patientId);
        // set gender, half male, half female
        if (count % 2 == 0) {
            patient.setGender(AdministrativeGender.FEMALE);
        } else {
            patient.setGender(AdministrativeGender.MALE);
        }
        // if testing attribution to generalPractitioner
        if (practitionerId != null) {
            patient.setGeneralPractitioner(Collections.singletonList(new Reference("Practitioner/" + practitionerId)));
        }
        // if testing attribution to managingOrganization
        if (organization != null) {
            patient.setManagingOrganization(new Reference("Organization/" + organization));
        }
        repository.create(patient);
    }

    public void createEncounter(
            String patientId,
            Period encounterPeriod,
            EncounterStatus encounterStatus,
            int patientCount,
            int encounterCount) {
        var encounterId = "Encounter/patient-" + patientCount + "-encounter-" + encounterCount;
        Encounter encounter = new Encounter();
        encounter.setId(encounterId);
        encounter.setPeriod(encounterPeriod);
        encounter.setStatus(encounterStatus);
        encounter.setSubject(new Reference(patientId));
        repository.create(encounter);
    }
}
