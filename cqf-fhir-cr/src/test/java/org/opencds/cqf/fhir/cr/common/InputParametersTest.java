package org.opencds.cqf.fhir.cr.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.repository.IRepository;
import com.google.common.collect.Multimap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.DataRequirement.DataRequirementCodeFilterComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionComponent;
import org.hl7.fhir.r4.model.ValueSet.ValueSetExpansionContainsComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;

@SuppressWarnings({"unchecked", "UnstableApiUsage"})
@ExtendWith(MockitoExtension.class)
class InputParametersTest {
    private final FhirContext fhirContextDstu3 = FhirContext.forDstu3Cached();
    private final FhirContext fhirContextR4 = FhirContext.forR4Cached();
    private final FhirContext fhirContextR5 = FhirContext.forR5Cached();
    private final org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory adapterFactoryDstu3 =
            new org.opencds.cqf.fhir.utility.adapter.dstu3.AdapterFactory();
    private final org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory adapterFactoryR4 =
            new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
    private final org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory adapterFactoryR5 =
            new org.opencds.cqf.fhir.utility.adapter.r5.AdapterFactory();
    private final String patientId = "patient1";
    private final String practitionerId = "practitioner1";
    private final String encounterId = "encounter1";
    private final String locationId = "location1";
    private final String studyId = "study1";

    @Mock
    IRepository repository;

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
        var resolver = IInputParameterResolver.createResolver(
                repository,
                patient.getIdElement(),
                Ids.newId(fhirContextDstu3, "Encounter", encounterId),
                practitioner.getIdElement(),
                null,
                null,
                // SDC Launch Context is not supported in Dstu3
                null,
                null);
        var actual = (org.hl7.fhir.dstu3.model.Parameters) resolver.getParameters();
        assertNotNull(actual);
        assertEquals(2, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(patient, actual.getParameter().get(0).getResource());
        assertEquals("%practitioner", actual.getParameter().get(1).getName());
        assertEquals(practitioner, actual.getParameter().get(1).getResource());
    }

    @Test
    void testResolveParametersR4() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setIdElement(Ids.newId(fhirContextR4, "Patient", patientId));
        var encounter = new org.hl7.fhir.r4.model.Encounter();
        encounter.setIdElement(Ids.newId(fhirContextR4, "Encounter", encounterId));
        var location = new org.hl7.fhir.r4.model.Location();
        location.setIdElement(Ids.newId(fhirContextR4, locationId));
        var practitioner = new org.hl7.fhir.r4.model.Practitioner();
        practitioner.setIdElement(Ids.newId(fhirContextR4, "Practitioner", practitionerId));
        var study = new org.hl7.fhir.r4.model.ResearchStudy();
        study.setIdElement(Ids.newId(fhirContextR4, "ResearchStudy", studyId));
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(patient).when(repository).read(org.hl7.fhir.r4.model.Patient.class, patient.getIdElement());
        doReturn(encounter).when(repository).read(org.hl7.fhir.r4.model.Encounter.class, encounter.getIdElement());
        doReturn(location).when(repository).read(org.hl7.fhir.r4.model.Location.class, location.getIdElement());
        doReturn(practitioner)
                .when(repository)
                .read(org.hl7.fhir.r4.model.Practitioner.class, practitioner.getIdElement());
        doReturn(study).when(repository).read(org.hl7.fhir.r4.model.ResearchStudy.class, study.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                patient.getIdElement(),
                null,
                null,
                null,
                null,
                Arrays.asList(
                        adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "patient"),
                                newPart(fhirContextR4, "Reference", "content", patient.getId()))),
                        adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "encounter"),
                                newPart(fhirContextR4, "Reference", "content", encounter.getId()))),
                        adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "location"),
                                newPart(fhirContextR4, "Reference", "content", location.getId()))),
                        adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "user"),
                                newPart(fhirContextR4, "Reference", "content", practitioner.getId()))),
                        adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR4,
                                "context",
                                newStringPart(fhirContextR4, "name", "study"),
                                newPart(fhirContextR4, "Reference", "content", study.getId())))),
                Arrays.asList(
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "name", new org.hl7.fhir.r4.model.Coding().setCode("patient")),
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "type", new org.hl7.fhir.r4.model.CodeType("Patient")))),
                        (IBaseExtension<?, ?>) new org.hl7.fhir.r4.model.Extension(
                                        Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r4.model.Extension(
                                                "name", new org.hl7.fhir.r4.model.Coding().setCode("encounter")),
                                        new org.hl7.fhir.r4.model.Extension(
                                                "type", new org.hl7.fhir.r4.model.CodeType("Encounter")))),
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "name", new org.hl7.fhir.r4.model.Coding().setCode("location")),
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "type", new org.hl7.fhir.r4.model.CodeType("Location")))),
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "name", new org.hl7.fhir.r4.model.Coding().setCode("user")),
                                                new org.hl7.fhir.r4.model.Extension(
                                                        "type", new org.hl7.fhir.r4.model.CodeType("Practitioner")))),
                        (IBaseExtension<?, ?>) new org.hl7.fhir.r4.model.Extension(
                                        Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r4.model.Extension(
                                                "name", new org.hl7.fhir.r4.model.Coding().setCode("study")),
                                        new org.hl7.fhir.r4.model.Extension(
                                                "type", new org.hl7.fhir.r4.model.CodeType("ResearchStudy"))))));
        var actual = (org.hl7.fhir.r4.model.Parameters) resolver.getParameters();
        assertNotNull(actual);
        assertEquals(11, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(patient, actual.getParameter().get(0).getResource());
        assertEquals("%patient", actual.getParameter().get(1).getName());
        assertEquals(patient, actual.getParameter().get(1).getResource());
        assertEquals("Patient", actual.getParameter().get(2).getName());
        assertEquals(patient, actual.getParameter().get(2).getResource());
        assertEquals("%encounter", actual.getParameter().get(3).getName());
        assertEquals(encounter, actual.getParameter().get(3).getResource());
        assertEquals("Encounter", actual.getParameter().get(4).getName());
        assertEquals(encounter, actual.getParameter().get(4).getResource());
        assertEquals("%location", actual.getParameter().get(5).getName());
        assertEquals(location, actual.getParameter().get(5).getResource());
        assertEquals("Location", actual.getParameter().get(6).getName());
        assertEquals(location, actual.getParameter().get(6).getResource());
        assertEquals("%user", actual.getParameter().get(7).getName());
        assertEquals(practitioner, actual.getParameter().get(7).getResource());
        assertEquals("User", actual.getParameter().get(8).getName());
        assertEquals(practitioner, actual.getParameter().get(8).getResource());
        assertEquals("%study", actual.getParameter().get(9).getName());
        assertEquals(study, actual.getParameter().get(9).getResource());
        assertEquals("Study", actual.getParameter().get(10).getName());
        assertEquals(study, actual.getParameter().get(10).getResource());
    }

    @Test
    void testUserLaunchContextAsPatientR4() {
        var user = new Patient();
        user.setIdElement(Ids.newId(fhirContextR4, "Patient", patientId));
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(user).when(repository).read(Patient.class, user.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                user.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "user"),
                        newPart(fhirContextR4, "Reference", "content", user.getId())))),
                List.of((IBaseExtension<?, ?>) new Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                        .setExtension(Arrays.asList(
                                new Extension("name", new Coding().setCode("user")),
                                new Extension("type", new CodeType("Patient"))))));
        var actual = (Parameters) resolver.getParameters();
        assertEquals(3, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(user, actual.getParameter().get(0).getResource());
        assertEquals("%user", actual.getParameter().get(1).getName());
        assertEquals(user, actual.getParameter().get(1).getResource());
        assertEquals("User", actual.getParameter().get(2).getName());
        assertEquals(user, actual.getParameter().get(2).getResource());
    }

    @Test
    void testUserLaunchContextAsPractitionerRoleR4() {
        var user = new PractitionerRole();
        user.setIdElement(Ids.newId(fhirContextR4, "PractitionerRole", practitionerId));
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(user).when(repository).read(PractitionerRole.class, user.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                user.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "user"),
                        newPart(fhirContextR4, "Reference", "content", user.getId())))),
                List.of((IBaseExtension<?, ?>) new Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                        .setExtension(Arrays.asList(
                                new Extension("name", new Coding().setCode("user")),
                                new Extension("type", new CodeType("PractitionerRole"))))));
        var actual = (Parameters) resolver.getParameters();
        assertEquals(3, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(user, actual.getParameter().get(0).getResource());
        assertEquals("%user", actual.getParameter().get(1).getName());
        assertEquals(user, actual.getParameter().get(1).getResource());
        assertEquals("User", actual.getParameter().get(2).getName());
        assertEquals(user, actual.getParameter().get(2).getResource());
    }

    @Test
    void testUserLaunchContextAsRelatedPersonR4() {
        var user = new org.hl7.fhir.r4.model.RelatedPerson();
        user.setIdElement(Ids.newId(fhirContextR4, "RelatedPerson", practitionerId));
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(user).when(repository).read(org.hl7.fhir.r4.model.RelatedPerson.class, user.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                user.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(adapterFactoryR4.createParametersParameter((IBaseBackboneElement) newPart(
                        fhirContextR4,
                        "context",
                        newStringPart(fhirContextR4, "name", "user"),
                        newPart(fhirContextR4, "Reference", "content", user.getId())))),
                List.of((IBaseExtension<?, ?>)
                        new org.hl7.fhir.r4.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r4.model.Extension(
                                                "name", new org.hl7.fhir.r4.model.Coding().setCode("user")),
                                        new org.hl7.fhir.r4.model.Extension(
                                                "type", new org.hl7.fhir.r4.model.CodeType("RelatedPerson"))))));
        var actual = (org.hl7.fhir.r4.model.Parameters) resolver.getParameters();
        assertEquals(3, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(user, actual.getParameter().get(0).getResource());
        assertEquals("%user", actual.getParameter().get(1).getName());
        assertEquals(user, actual.getParameter().get(1).getResource());
        assertEquals("User", actual.getParameter().get(2).getName());
        assertEquals(user, actual.getParameter().get(2).getResource());
    }

    @Test
    void testResolveParametersR5() {
        var patient = new org.hl7.fhir.r5.model.Patient();
        patient.setIdElement(Ids.newId(fhirContextR5, "Patient", patientId));
        var encounter = new org.hl7.fhir.r5.model.Encounter();
        encounter.setIdElement(Ids.newId(fhirContextR5, "Encounter", encounterId));
        var location = new org.hl7.fhir.r5.model.Location();
        location.setIdElement(Ids.newId(fhirContextR5, locationId));
        var practitioner = new org.hl7.fhir.r5.model.Practitioner();
        practitioner.setIdElement(Ids.newId(fhirContextR5, "Practitioner", practitionerId));
        var study = new org.hl7.fhir.r5.model.ResearchStudy();
        study.setIdElement(Ids.newId(fhirContextR5, "ResearchStudy", studyId));
        doReturn(fhirContextR5).when(repository).fhirContext();
        doReturn(patient).when(repository).read(org.hl7.fhir.r5.model.Patient.class, patient.getIdElement());
        doReturn(encounter).when(repository).read(org.hl7.fhir.r5.model.Encounter.class, encounter.getIdElement());
        doReturn(location).when(repository).read(org.hl7.fhir.r5.model.Location.class, location.getIdElement());
        doReturn(practitioner)
                .when(repository)
                .read(org.hl7.fhir.r5.model.Practitioner.class, practitioner.getIdElement());
        doReturn(study).when(repository).read(org.hl7.fhir.r5.model.ResearchStudy.class, study.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                patient.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(
                        adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "patient"),
                                newPart(fhirContextR5, "Reference", "content", patient.getId()))),
                        adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "encounter"),
                                newPart(fhirContextR5, "Reference", "content", encounter.getId()))),
                        adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "location"),
                                newPart(fhirContextR5, "Reference", "content", location.getId()))),
                        adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "user"),
                                newPart(fhirContextR5, "Reference", "content", practitioner.getId()))),
                        adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                                fhirContextR5,
                                "context",
                                newStringPart(fhirContextR5, "name", "study"),
                                newPart(fhirContextR5, "Reference", "content", study.getId())))),
                List.of(
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "name", new org.hl7.fhir.r5.model.Coding().setCode("patient")),
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "type", new org.hl7.fhir.r5.model.CodeType("Patient")))),
                        (IBaseExtension<?, ?>) new org.hl7.fhir.r5.model.Extension(
                                        Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r5.model.Extension(
                                                "name", new org.hl7.fhir.r5.model.Coding().setCode("encounter")),
                                        new org.hl7.fhir.r5.model.Extension(
                                                "type", new org.hl7.fhir.r5.model.CodeType("Encounter")))),
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "name", new org.hl7.fhir.r5.model.Coding().setCode("location")),
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "type", new org.hl7.fhir.r5.model.CodeType("Location")))),
                        (IBaseExtension<?, ?>)
                                new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                        .setExtension(Arrays.asList(
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "name", new org.hl7.fhir.r5.model.Coding().setCode("user")),
                                                new org.hl7.fhir.r5.model.Extension(
                                                        "type", new org.hl7.fhir.r5.model.CodeType("Practitioner")))),
                        (IBaseExtension<?, ?>) new org.hl7.fhir.r5.model.Extension(
                                        Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r5.model.Extension(
                                                "name", new org.hl7.fhir.r5.model.Coding().setCode("study")),
                                        new org.hl7.fhir.r5.model.Extension(
                                                "type", new org.hl7.fhir.r5.model.CodeType("ResearchStudy"))))));
        var actual = (org.hl7.fhir.r5.model.Parameters) resolver.getParameters();
        assertNotNull(actual);
        assertEquals(11, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(patient, actual.getParameter().get(0).getResource());
        assertEquals("%patient", actual.getParameter().get(1).getName());
        assertEquals(patient, actual.getParameter().get(1).getResource());
        assertEquals("Patient", actual.getParameter().get(2).getName());
        assertEquals(patient, actual.getParameter().get(2).getResource());
        assertEquals("%encounter", actual.getParameter().get(3).getName());
        assertEquals(encounter, actual.getParameter().get(3).getResource());
        assertEquals("Encounter", actual.getParameter().get(4).getName());
        assertEquals(encounter, actual.getParameter().get(4).getResource());
        assertEquals("%location", actual.getParameter().get(5).getName());
        assertEquals(location, actual.getParameter().get(5).getResource());
        assertEquals("Location", actual.getParameter().get(6).getName());
        assertEquals(location, actual.getParameter().get(6).getResource());
        assertEquals("%user", actual.getParameter().get(7).getName());
        assertEquals(practitioner, actual.getParameter().get(7).getResource());
        assertEquals("User", actual.getParameter().get(8).getName());
        assertEquals(practitioner, actual.getParameter().get(8).getResource());
        assertEquals("%study", actual.getParameter().get(9).getName());
        assertEquals(study, actual.getParameter().get(9).getResource());
        assertEquals("Study", actual.getParameter().get(10).getName());
        assertEquals(study, actual.getParameter().get(10).getResource());
    }

    @Test
    void testUserLaunchContextAsPatientR5() {
        var user = new org.hl7.fhir.r5.model.Patient();
        user.setIdElement(Ids.newId(fhirContextR5, "Patient", patientId));
        doReturn(fhirContextR5).when(repository).fhirContext();
        doReturn(user).when(repository).read(org.hl7.fhir.r5.model.Patient.class, user.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                user.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                        fhirContextR5,
                        "context",
                        newStringPart(fhirContextR5, "name", "user"),
                        newPart(fhirContextR5, "Reference", "content", user.getId())))),
                List.of((IBaseExtension<?, ?>)
                        new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r5.model.Extension(
                                                "name", new org.hl7.fhir.r5.model.Coding().setCode("user")),
                                        new org.hl7.fhir.r5.model.Extension(
                                                "type", new org.hl7.fhir.r5.model.CodeType("Patient"))))));
        var actual = (org.hl7.fhir.r5.model.Parameters) resolver.getParameters();
        assertEquals(3, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(user, actual.getParameter().get(0).getResource());
        assertEquals("%user", actual.getParameter().get(1).getName());
        assertEquals(user, actual.getParameter().get(1).getResource());
        assertEquals("User", actual.getParameter().get(2).getName());
        assertEquals(user, actual.getParameter().get(2).getResource());
    }

    @Test
    void testUserLaunchContextAsPractitionerRoleR5() {
        var user = new org.hl7.fhir.r5.model.PractitionerRole();
        user.setIdElement(Ids.newId(fhirContextR5, "PractitionerRole", practitionerId));
        doReturn(fhirContextR5).when(repository).fhirContext();
        doReturn(user).when(repository).read(org.hl7.fhir.r5.model.PractitionerRole.class, user.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                user.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                        fhirContextR5,
                        "context",
                        newStringPart(fhirContextR5, "name", "user"),
                        newPart(fhirContextR5, "Reference", "content", user.getId())))),
                List.of((IBaseExtension<?, ?>)
                        new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r5.model.Extension(
                                                "name", new org.hl7.fhir.r5.model.Coding().setCode("user")),
                                        new org.hl7.fhir.r5.model.Extension(
                                                "type", new org.hl7.fhir.r5.model.CodeType("PractitionerRole"))))));
        var actual = (org.hl7.fhir.r5.model.Parameters) resolver.getParameters();
        assertEquals(3, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(user, actual.getParameter().get(0).getResource());
        assertEquals("%user", actual.getParameter().get(1).getName());
        assertEquals(user, actual.getParameter().get(1).getResource());
        assertEquals("User", actual.getParameter().get(2).getName());
        assertEquals(user, actual.getParameter().get(2).getResource());
    }

    @Test
    void testUserLaunchContextAsRelatedPersonR5() {
        var user = new org.hl7.fhir.r5.model.RelatedPerson();
        user.setIdElement(Ids.newId(fhirContextR5, "RelatedPerson", practitionerId));
        doReturn(fhirContextR5).when(repository).fhirContext();
        doReturn(user).when(repository).read(org.hl7.fhir.r5.model.RelatedPerson.class, user.getIdElement());
        var resolver = IInputParameterResolver.createResolver(
                repository,
                user.getIdElement(),
                null,
                null,
                null,
                null,
                List.of(adapterFactoryR5.createParametersParameter((IBaseBackboneElement) newPart(
                        fhirContextR5,
                        "context",
                        newStringPart(fhirContextR5, "name", "user"),
                        newPart(fhirContextR5, "Reference", "content", user.getId())))),
                List.of((IBaseExtension<?, ?>)
                        new org.hl7.fhir.r5.model.Extension(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT)
                                .setExtension(Arrays.asList(
                                        new org.hl7.fhir.r5.model.Extension(
                                                "name", new org.hl7.fhir.r5.model.Coding().setCode("user")),
                                        new org.hl7.fhir.r5.model.Extension(
                                                "type", new org.hl7.fhir.r5.model.CodeType("RelatedPerson"))))));
        var actual = (org.hl7.fhir.r5.model.Parameters) resolver.getParameters();
        assertEquals(3, actual.getParameter().size());
        assertEquals("%subject", actual.getParameter().get(0).getName());
        assertEquals(user, actual.getParameter().get(0).getResource());
        assertEquals("%user", actual.getParameter().get(1).getName());
        assertEquals(user, actual.getParameter().get(1).getResource());
        assertEquals("User", actual.getParameter().get(2).getName());
        assertEquals(user, actual.getParameter().get(2).getResource());
    }

    @Test
    void testResolveInputParametersWithDataRequirements() {
        var patient = new org.hl7.fhir.r4.model.Patient();
        patient.setIdElement(Ids.newId(fhirContextR4, "Patient", patientId));
        var valueSetUrl = "ValueSet/testValueSet";
        var valueSet = new ValueSet()
                .setUrl(valueSetUrl)
                .setExpansion(new ValueSetExpansionComponent()
                        .addContains(new ValueSetExpansionContainsComponent()
                                .setCode("testCode")
                                .setSystem("testSystem")));
        var valueSetBundle = new Bundle().addEntry(new BundleEntryComponent().setResource(valueSet));
        var obs = new Observation()
                .setCode(new CodeableConcept()
                        .addCoding(new Coding().setCode("testCode").setSystem("testSystem")));
        var obsBundle = new Bundle().addEntry(new BundleEntryComponent().setResource(obs));
        doReturn(fhirContextR4).when(repository).fhirContext();
        doReturn(patient).when(repository).read(org.hl7.fhir.r4.model.Patient.class, patient.getIdElement());
        doReturn(valueSetBundle).when(repository).search(eq(Bundle.class), eq(ValueSet.class), any(Map.class));
        doReturn(obsBundle)
                .when(repository)
                .search(eq(Bundle.class), eq(Observation.class), any(Multimap.class), any());
        var resolver = IInputParameterResolver.createResolver(
                repository, patient.getIdElement(), null, null, null, null, null, null);
        var inputReq = new DataRequirement()
                .setType("Observation")
                .addCodeFilter(
                        new DataRequirementCodeFilterComponent().setPath("code").setValueSet(valueSetUrl));
        inputReq.setId("test");
        var actual = (Parameters) resolver.resolveInputParameters(List.of(inputReq));
        assertNotNull(actual);
        var parameter = actual.getParameter("%" + inputReq.getId());
        assertNotNull(parameter);
        assertEquals(obs, parameter.getResource());
    }
}
