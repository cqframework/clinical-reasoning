package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.helpers.RequestHelpers;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Constants.CPG_ACTIVITY_TYPE_CODE;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

@SuppressWarnings("UnstableApiUsage")
@ExtendWith(MockitoExtension.class)
class ActionResolverTests {

    @Mock
    private IRepository repository;

    @Mock
    private LibraryEngine libraryEngine;

    @Mock
    private IInputParameterResolver inputParameterResolver;

    private final ActionResolver fixture = new ActionResolver();

    @Test
    void testQuestionnaireTaskWithNoFocusGetsAssigned() {
        var fhirVersion = FhirVersionEnum.R4;
        var questionnaire = new org.hl7.fhir.r4.model.Questionnaire().setUrl("test.com/Questionnaire/test");
        var task = new org.hl7.fhir.r4.model.Task()
                .setCode(new CodeableConcept(new Coding(
                        Constants.CPG_ACTIVITY_TYPE_CS,
                        CPG_ACTIVITY_TYPE_CODE.COLLECT_INFORMATION.code,
                        CPG_ACTIVITY_TYPE_CODE.COLLECT_INFORMATION.name())));
        var requestGroup = new org.hl7.fhir.r4.model.RequestGroup();
        requestGroup.setId("RequestGroup/TestQuestionnaireTask");
        var request = RequestHelpers.newPDApplyRequestForVersion(
                        fhirVersion,
                        libraryEngine,
                        FhirModelResolverCache.resolverForVersion(fhirVersion),
                        null,
                        inputParameterResolver)
                .setQuestionnaire(questionnaire);
        var action = (IPlanDefinitionActionAdapter) IAdapterFactory.createAdapterForBase(
                fhirVersion,
                new org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent().setId("collect-information"));
        fixture.resolveTask(request, requestGroup, task, action);
        assertEquals(questionnaire.getUrl(), task.getFocus().getReference());
    }
}
