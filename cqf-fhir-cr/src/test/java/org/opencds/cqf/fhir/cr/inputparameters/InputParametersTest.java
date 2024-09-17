package org.opencds.cqf.fhir.cr.inputparameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;

@ExtendWith(MockitoExtension.class)
class InputParametersTest {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final String patientId = "patient1";
    private final String practitionerId = "practitioner1";
    private final String encounterId = "encounter1";

    @Mock
    Repository repository;

    @Test
    void testResolveParametersDstu3() {
        var patient = new org.hl7.fhir.dstu3.model.Patient();
        patient.setIdElement(Ids.newId(fhirContextDstu3, "Patient", patientId));
        var practitioner = new org.hl7.fhir.dstu3.model.Practitioner();
        practitioner.setIdElement(Ids.newId(fhirContextDstu3, "Practitioner", practitionerId));
        doReturn(fhirContextDstu3).when(repository).fhirContext();
        doReturn(patient).when(repository).read(org.hl7.fhir.dstu3.model.Patient.class, patient.getIdElement());
        doReturn(practitioner)
                .when(repository)
                .read(org.hl7.fhir.dstu3.model.Practitioner.class, practitioner.getIdElement());
        var resolver = new org.opencds.cqf.fhir.cr.inputparameters.dstu3.InputParameterResolver(
                repository,
                patient.getIdElement(),
                Ids.newId(fhirContextDstu3, "Encounter", encounterId),
                practitioner.getIdElement(),
                null,
                true,
                null,
                // SDC Launch Context is not supported in Dstu3
                null,
                null);
        var actual = resolver.getParameters();
        assertNotNull(actual);
        assertEquals(2, actual.getParameter().size());
        assertTrue(actual.getParameter().get(0).getName().equals("%subject"));
        assertTrue(actual.getParameter().get(0).getResource().equals(patient));
        assertTrue(actual.getParameter().get(1).getName().equals("%practitioner"));
        assertTrue(actual.getParameter().get(1).getResource().equals(practitioner));
    }

    @Test
    void testResolveParametersR4() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setIdElement(Ids.newId(fhirContextR4, "Patient", patientId));
        var practitioner = new org.hl7.fhir.r4.model.Practitioner();
        practitioner.setIdElement(Ids.newId(fhirContextR4, "Practitioner", practitionerId));
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(patient).when(repository).read(org.hl7.fhir.r4.model.Patient.class, patient.getIdElement());
        doReturn(practitioner)
                .when(repository)
                .read(org.hl7.fhir.r4.model.Practitioner.class, practitioner.getIdElement());
        var resolver = new org.opencds.cqf.fhir.cr.inputparameters.r4.InputParameterResolver(
                repository,
                patient.getIdElement(),
                Ids.newId(fhirContextR4, "Encounter", encounterId),
                practitioner.getIdElement(),
                null,
                true,
                null,
                Arrays.asList(
                        newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "patient"),
                                newPart(fhirContextR4, "Reference", "content", patient.getId())),
                        newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "user"),
                                newPart(fhirContextR4, "Reference", "content", practitioner.getId()))),
                Arrays.asList(
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "name", new org.hl7.fhir.r4.model.Coding().setCode("patient")),
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "type", new org.hl7.fhir.r4.model.CodeType("Patient")))),
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "name", new org.hl7.fhir.r4.model.Coding().setCode("user")),
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "type", new org.hl7.fhir.r4.model.CodeType("Practitioner"))))));
        var actual = resolver.getParameters();
        assertNotNull(actual);
        assertEquals(6, actual.getParameter().size());
        assertTrue(actual.getParameter().get(0).getName().equals("%subject"));
        assertTrue(actual.getParameter().get(0).getResource().equals(patient));
        assertTrue(actual.getParameter().get(1).getName().equals("%practitioner"));
        assertTrue(actual.getParameter().get(1).getResource().equals(practitioner));
        assertTrue(actual.getParameter().get(2).getName().equals("%patient"));
        assertTrue(actual.getParameter().get(2).getResource().equals(patient));
        assertTrue(actual.getParameter().get(3).getName().equals("Patient"));
        assertTrue(actual.getParameter().get(3).getResource().equals(patient));
        assertTrue(actual.getParameter().get(4).getName().equals("%user"));
        assertTrue(actual.getParameter().get(4).getResource().equals(practitioner));
        assertTrue(actual.getParameter().get(5).getName().equals("User"));
        assertTrue(actual.getParameter().get(5).getResource().equals(practitioner));
    }

    @Test
    void testResolveParametersR5() {
        var patient = new org.hl7.fhir.r5.model.Patient();
        patient.setIdElement(Ids.newId(fhirContextR5, "Patient", patientId));
        var practitioner = new org.hl7.fhir.r5.model.Practitioner();
        practitioner.setIdElement(Ids.newId(fhirContextR5, "Practitioner", practitionerId));
        doReturn(fhirContextR5).when(repository).fhirContext();
        doReturn(patient).when(repository).read(org.hl7.fhir.r5.model.Patient.class, patient.getIdElement());
        doReturn(practitioner)
                .when(repository)
                .read(org.hl7.fhir.r5.model.Practitioner.class, practitioner.getIdElement());
        var resolver = new org.opencds.cqf.fhir.cr.inputparameters.r5.InputParameterResolver(
                repository,
                patient.getIdElement(),
                Ids.newId(fhirContextR5, "Encounter", encounterId),
                practitioner.getIdElement(),
                null,
                true,
                null,
                Arrays.asList(
                        newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "patient"),
                                newPart(fhirContextR5, "Reference", "content", patient.getId())),
                        newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "user"),
                                newPart(fhirContextR5, "Reference", "content", practitioner.getId()))),
                Arrays.asList(
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "name", new org.hl7.fhir.r5.model.Coding().setCode("patient")),
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "type", new org.hl7.fhir.r5.model.CodeType("Patient")))),
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "name", new org.hl7.fhir.r5.model.Coding().setCode("user")),
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "type", new org.hl7.fhir.r5.model.CodeType("Practitioner"))))));
        var actual = resolver.getParameters();
        assertNotNull(actual);
        assertEquals(6, actual.getParameter().size());
        assertTrue(actual.getParameter().get(0).getName().equals("%subject"));
        assertTrue(actual.getParameter().get(0).getResource().equals(patient));
        assertTrue(actual.getParameter().get(1).getName().equals("%practitioner"));
        assertTrue(actual.getParameter().get(1).getResource().equals(practitioner));
        assertTrue(actual.getParameter().get(2).getName().equals("%patient"));
        assertTrue(actual.getParameter().get(2).getResource().equals(patient));
        assertTrue(actual.getParameter().get(3).getName().equals("Patient"));
        assertTrue(actual.getParameter().get(3).getResource().equals(patient));
        assertTrue(actual.getParameter().get(4).getName().equals("%user"));
        assertTrue(actual.getParameter().get(4).getResource().equals(practitioner));
        assertTrue(actual.getParameter().get(5).getName().equals("User"));
        assertTrue(actual.getParameter().get(5).getResource().equals(practitioner));
    }
}
