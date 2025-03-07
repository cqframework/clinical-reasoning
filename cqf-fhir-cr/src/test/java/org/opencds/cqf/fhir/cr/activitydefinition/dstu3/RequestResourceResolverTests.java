package org.opencds.cqf.fhir.cr.activitydefinition.dstu3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.dstu3.model.Communication;
import org.hl7.fhir.dstu3.model.CommunicationRequest;
import org.hl7.fhir.dstu3.model.DiagnosticReport;
import org.hl7.fhir.dstu3.model.Encounter;
import org.hl7.fhir.dstu3.model.MedicationRequest;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Practitioner;
import org.hl7.fhir.dstu3.model.Procedure;
import org.hl7.fhir.dstu3.model.ProcedureRequest;
import org.hl7.fhir.dstu3.model.ReferralRequest;
import org.hl7.fhir.dstu3.model.SupplyRequest;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.fhir.cr.activitydefinition.RequestResourceResolver.Given;
import org.opencds.cqf.fhir.utility.Ids;

@TestInstance(Lifecycle.PER_CLASS)
class RequestResourceResolverTests {
    private final FhirContext fhirContext = FhirContext.forDstu3Cached();
    private final IIdType subjectId = Ids.newId(Patient.class, "patient123");
    private final IIdType practitionerId = Ids.newId(Practitioner.class, "practitioner123");
    private final IIdType encounterId = Ids.newId(Encounter.class, "encounter123");
    private final IIdType organizationId = Ids.newId(Organization.class, "org123");

    private <R extends IBaseResource> R testResolver(String testId, Class<R> expectedClass) {
        return testResolver(testId, expectedClass, subjectId, practitionerId, encounterId, organizationId);
    }

    @SuppressWarnings("unchecked")
    private <R extends IBaseResource> R testResolver(
            String testId,
            Class<R> expectedClass,
            IIdType subjectId,
            IIdType practitionerId,
            IIdType encounterId,
            IIdType organizationId) {
        var result = new Given()
                .repositoryFor(fhirContext, "dstu3")
                .activityDefinition(testId)
                .when()
                .subjectId(subjectId)
                .encounterId(encounterId)
                .practitionerId(practitionerId)
                .organizationId(organizationId)
                .resolve();
        assertNotNull(result);
        assertEquals(expectedClass, (Class<R>) result.getClass());

        return (R) result;
    }

    @Test
    void communicationRequestResolver() {
        testResolver("communicationrequest-test", CommunicationRequest.class);
    }

    @Test
    void communicationResolver() {
        testResolver("communication-test", Communication.class);
    }

    @Test
    void diagnosticReportResolver() {
        testResolver("diagnosticreport-test", DiagnosticReport.class);
    }

    @Test
    void medicationRequestResolver() {
        testResolver("medicationrequest-test", MedicationRequest.class);
    }

    @Test
    void procedureRequestResolver() {
        testResolver("procedurerequest-test", ProcedureRequest.class);
    }

    @Test
    void procedureResolver() {
        testResolver("procedure-test", Procedure.class);
    }

    @Test
    void referralRequestResolver() {
        testResolver("referralrequest-test", ReferralRequest.class);
        testResolver("referralrequest-test", ReferralRequest.class, subjectId, null, encounterId, organizationId);
        testResolver("referralrequest-test", ReferralRequest.class, subjectId, null, encounterId, null);
    }

    @Test
    void supplyRequestResolver() {
        testResolver("supplyrequest-test", SupplyRequest.class);
    }

    @Test
    void taskResolver() {
        testResolver("task-test", Task.class);
    }

    @Test
    void unsupported() {
        assertThrows(FHIRException.class, () -> {
            testResolver("unsupported-test", Task.class);
        });
    }
}
