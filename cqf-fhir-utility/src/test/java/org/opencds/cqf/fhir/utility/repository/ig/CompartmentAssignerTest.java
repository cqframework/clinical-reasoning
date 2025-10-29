package org.opencds.cqf.fhir.utility.repository.ig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.IQueryParameterType;
import ca.uhn.fhir.rest.param.ReferenceParam;
import com.google.common.collect.ArrayListMultimap;
import java.util.List;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.utility.repository.ig.IgConventions.CompartmentIsolation;

class CompartmentAssignerTest {

    @Test
    void patientAssignedToOwnCompartment() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var patient = new Patient();
        patient.setId("example");

        var assignment = assigner.fromResource(patient);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("example", assignment.compartmentId());
    }

    @Test
    void observationUsesSubjectReference() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var observation = new Observation();
        observation.setId("obs-1");
        observation.setSubject(new Reference("Patient/123"));

        var assignment = assigner.fromResource(observation);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("123", assignment.compartmentId());
    }

    @Test
    void coveragePrefersBeneficiaryOverOtherReferences() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var coverage = new Coverage();
        coverage.setId("cov-1");
        coverage.setBeneficiary(new Reference("Patient/alpha"));
        coverage.setPolicyHolder(new Reference("Patient/beta"));

        var assignment = assigner.fromResource(coverage);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("alpha", assignment.compartmentId());
    }

    @Test
    void observationSearchWithSubjectResolvesCompartment() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        params.put("subject", List.of(new ReferenceParam("Patient/777")));

        var assignment = assigner.fromSearchParameters("Observation", params);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("777", assignment.compartmentId());
    }

    @Test
    void ambiguousSearchDoesNotMatch() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        // The "subject" parameter could refer to multiple compartment types (e.g. Patient, Device)
        params.put("subject", List.of(new ReferenceParam("999")));

        var assignment = assigner.fromSearchParameters("Observation", params);

        assertTrue(assignment.isUnknown());
    }

    @Test
    void patientSearchMatches() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        // The "patient" parameter specifically refers to the Patient compartment
        params.put("patient", List.of(new ReferenceParam("999")));

        var assignment = assigner.fromSearchParameters("Observation", params);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("999", assignment.compartmentId());
    }

    @Test
    void blankSubjectSearchReturnsUnknownCompartment() {
        var assigner =
                new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT, CompartmentIsolation.FHIR);
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        params.put("subject", List.of(new ReferenceParam("")));

        var assignment = assigner.fromSearchParameters("Observation", params);

        assertTrue(assignment.isUnknown());
    }
}
