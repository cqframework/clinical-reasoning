package org.opencds.cqf.fhir.cr.activitydefinition.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.api.EncodingEnum;
import java.lang.reflect.InvocationTargetException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.AppointmentResponse;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.Communication;
import org.hl7.fhir.r4.model.CommunicationRequest;
import org.hl7.fhir.r4.model.Contract;
import org.hl7.fhir.r4.model.DeviceRequest;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.EnrollmentRequest;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ImmunizationRecommendation;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.NutritionOrder;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.RequestGroup;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.VisionPrescription;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.BaseRequestResourceResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.AppointmentResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.AppointmentResponseResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.CarePlanResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ClaimResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.CommunicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.CommunicationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ContractResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.DeviceRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.DiagnosticReportResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.EnrollmentRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ImmunizationRecommendationResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.MedicationRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.NutritionOrderResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ProcedureResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.RequestGroupResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.ServiceRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.SupplyRequestResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.TaskResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.r4.VisionPrescriptionResolver;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.repository.IGFileStructureRepository;
import org.opencds.cqf.fhir.utility.repository.IGLayoutMode;

@TestInstance(Lifecycle.PER_CLASS)
@Disabled
public class RequestResourceResolverTests {
    private final FhirContext fhirContext = FhirContext.forR4Cached();
    // private final IParser parser = fhirContext.newJsonParser();
    private final IIdType subjectId = Ids.newId(Patient.class, "patient123");
    private final IIdType practitionerId = Ids.newId(Practitioner.class, "practitioner123");
    private final IIdType encounterId = Ids.newId(Encounter.class, "encounter123");
    private final IIdType organizationId = Ids.newId(Organization.class, "org123");

    private Repository repository;

    @BeforeAll
    public void setup() {
        repository = new IGFileStructureRepository(
                this.fhirContext,
                this.getClass()
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .getPath() + "org/opencds/cqf/fhir/cr/activitydefinition/r4",
                IGLayoutMode.TYPE_PREFIX,
                EncodingEnum.JSON);
    }

    @SuppressWarnings("unchecked")
    private <R extends IBaseResource> R testResolver(
            String testId, Class<? extends BaseRequestResourceResolver> clazz, Class<R> expectedClass) {
        // String actual = null;
        // String expected = null;
        IBaseResource result = null;
        var activityDefinition = repository.read(ActivityDefinition.class, new IdType("ActivityDefinition", testId));
        try {
            var resolver = clazz.getConstructor(ActivityDefinition.class).newInstance(activityDefinition);
            result = resolver.resolve(subjectId, practitionerId, encounterId, organizationId);
            // actual = parser.encodeResourceToString(result);
            // expected = parser.encodeResourceToString(repository.read(expectedClass, new IdType(testId)));
        } catch (DataFormatException
                | IllegalAccessException
                | IllegalArgumentException
                | InstantiationException
                | NoSuchMethodException
                | SecurityException
                | InvocationTargetException e) {
        }
        Assertions.assertNotNull(result);
        // Assertions.assertEquals(expected, actual);

        return (R) result;
    }

    @Test
    public void testAppointmentResolver() {
        testResolver("appointment-test", AppointmentResolver.class, Appointment.class);
    }

    @Test
    public void testAppointmentResponseResolver() {
        testResolver("appointmentresponse-test", AppointmentResponseResolver.class, AppointmentResponse.class);
    }

    @Test
    public void testCarePlanResolver() {
        testResolver("careplan-test", CarePlanResolver.class, CarePlan.class);
    }

    @Test
    public void testClaimResolver() {
        testResolver("claim-test", ClaimResolver.class, Claim.class);
    }

    @Test
    public void testCommunicationRequestResolver() {
        testResolver("communicationrequest-test", CommunicationRequestResolver.class, CommunicationRequest.class);
    }

    @Test
    @Disabled("Communication is not a valid value for ActivityDefinitionKind")
    public void testCommunicationResolver() {
        testResolver("communication-test", CommunicationResolver.class, Communication.class);
    }

    @Test
    public void testContractResolver() {
        testResolver("contract-test", ContractResolver.class, Contract.class);
    }

    @Test
    public void testDeviceRequestResolver() {
        testResolver("devicerequest-test", DeviceRequestResolver.class, DeviceRequest.class);
    }

    @Test
    @Disabled("DiagnosticReport is not a valid value for ActivityDefinitionKind")
    public void testDiagnosticReportResolver() {
        testResolver("diagnosticreport-test", DiagnosticReportResolver.class, DiagnosticReport.class);
    }

    @Test
    public void testEnrollmentRequestResolver() {
        testResolver("enrollmentrequest-test", EnrollmentRequestResolver.class, EnrollmentRequest.class);
    }

    @Test
    public void testImmunizationRecommendationResolver() {
        testResolver(
                "immunizationrecommendation-test",
                ImmunizationRecommendationResolver.class,
                ImmunizationRecommendation.class);
    }

    @Test
    public void testMedicationRequestResolver() {
        testResolver("medicationrequest-test", MedicationRequestResolver.class, MedicationRequest.class);
    }

    @Test
    public void testNutritionOrderResolver() {
        testResolver("nutritionorder-test", NutritionOrderResolver.class, NutritionOrder.class);
    }

    @Test
    @Disabled("Procedure is not a valid value for ActivityDefinitionKind")
    public void testProcedureResolver() {
        testResolver("procedure-test", ProcedureResolver.class, Procedure.class);
    }

    @Test
    @Disabled("RequestGroup is not a valid value for ActivityDefinitionKind")
    public void testRequestGroupResolver() {
        testResolver("requestgroup-test", RequestGroupResolver.class, RequestGroup.class);
    }

    @Test
    public void testServiceRequestResolver() {
        testResolver("servicerequest-test", ServiceRequestResolver.class, ServiceRequest.class);
    }

    @Test
    public void testSupplyRequestResolver() {
        testResolver("supplyrequest-test", SupplyRequestResolver.class, SupplyRequest.class);
    }

    @Test
    public void testTaskResolver() {
        testResolver("task-test", TaskResolver.class, Task.class);
    }

    @Test
    public void testVisionPrescriptionResolver() {
        testResolver("visionprescription-test", VisionPrescriptionResolver.class, VisionPrescription.class);
    }
}
