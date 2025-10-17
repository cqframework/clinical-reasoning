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

class CompartmentAssignerTest {

    private CompartmentAssigner assigner = new CompartmentAssigner(FhirContext.forR4Cached(), CompartmentMode.PATIENT);

    @Test
    void patientAssignedToOwnCompartment() {
        var patient = new Patient();
        patient.setId("example");

        var assignment = assigner.assign(patient);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("example", assignment.compartmentId());
    }

    @Test
    void observationUsesSubjectReference() {
        var observation = new Observation();
        observation.setId("obs-1");
        observation.setSubject(new Reference("Patient/123"));

        var assignment = assigner.assign(observation);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("123", assignment.compartmentId());
    }

    @Test
    void coveragePrefersBeneficiaryOverOtherReferences() {
        var coverage = new Coverage();
        coverage.setId("cov-1");
        coverage.setBeneficiary(new Reference("Patient/alpha"));
        coverage.setPolicyHolder(new Reference("Patient/beta"));

        var assignment = assigner.assign(coverage);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("alpha", assignment.compartmentId());
    }

    @Test
    void observationSearchWithSubjectResolvesCompartment() {
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        params.put("subject", List.of(new ReferenceParam("Patient/777")));

        var assignment = assigner.assign("Observation", params);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("777", assignment.compartmentId());
    }

    @Test
    void ambiguousSearchDoesNotMatch() {
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        // The "subject" parameter could refer to multiple compartment types (e.g. Patient, Device)
        params.put("subject", List.of(new ReferenceParam("999")));

        var assignment = assigner.assign("Observation", params);

        assertTrue(assignment.isUnknown());
    }

    @Test
    void patientSearchMatches() {
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        // The "patient" parameter specifically refers to the Patient compartment
        params.put("patient", List.of(new ReferenceParam("999")));

        var assignment = assigner.assign("Observation", params);

        assertEquals("patient", assignment.compartmentType());
        assertEquals("999", assignment.compartmentId());
    }

    @Test
    void blankSubjectSearchReturnsUnknownCompartment() {
        var params = ArrayListMultimap.<String, List<IQueryParameterType>>create();
        params.put("subject", List.of(new ReferenceParam("")));

        var assignment = assigner.assign("Observation", params);

        assertTrue(assignment.isUnknown());
    }
}
