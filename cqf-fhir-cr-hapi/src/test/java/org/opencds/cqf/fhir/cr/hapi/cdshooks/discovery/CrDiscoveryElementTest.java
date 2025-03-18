package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery.CrDiscoveryElement.getCdsServiceJson;
import static org.opencds.cqf.fhir.utility.Constants.CRMI_EFFECTIVE_DATA_REQUIREMENTS;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.ClasspathUtil;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r4.model.TriggerDefinition;
import org.hl7.fhir.r4.model.TriggerDefinition.TriggerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opencds.cqf.fhir.cr.hapi.cdshooks.BaseCdsCrTest;

class CrDiscoveryElementTest extends BaseCdsCrTest {

    @BeforeEach
    void beforeEach() {
        fhirContext = FhirContext.forR4Cached();
        repository = getRepository();
        restfulServer = getRestfulServer();
        adapterFactory = getAdapterFactory();
    }

    @Test
    void testNullArguments() {
        assertNull(getCdsServiceJson(null, null));
        var planDefNoAction = adapterFactory.createPlanDefinition(new PlanDefinition());
        assertNull(getCdsServiceJson(planDefNoAction, null));
        var planDefActionNoTrigger = adapterFactory.createPlanDefinition(
                new PlanDefinition().addAction(new PlanDefinitionActionComponent().setDescription("test")));
        assertNull(getCdsServiceJson(planDefActionNoTrigger, null));
        var planDefActionInvalidTrigger = adapterFactory.createPlanDefinition(new PlanDefinition()
                .addAction(new PlanDefinitionActionComponent()
                        .setDescription("test")
                        .addTrigger(new TriggerDefinition())));
        assertNull(getCdsServiceJson(planDefActionInvalidTrigger, null));
        var planDefActionTriggerNoInfo = adapterFactory.createPlanDefinition(new PlanDefinition()
                .addAction(new PlanDefinitionActionComponent()
                        .setDescription("test")
                        .addTrigger(new TriggerDefinition().setType(TriggerType.NAMEDEVENT))));
        var response = getCdsServiceJson(planDefActionTriggerNoInfo, null);
        assertNotNull(response);
        assertEquals("Patient?_id={{context.patientId}}", response.getPrefetch().get("item1"));
    }

    @Test
    void testPrefetchUrlList() {
        var title = "Test Title";
        var hook = "patient-view";
        var planDefinition = new PlanDefinition()
                .setTitle(title)
                .addAction(new PlanDefinitionActionComponent()
                        .setDescription("test")
                        .addTrigger(new TriggerDefinition()
                                .setType(TriggerType.NAMEDEVENT)
                                .setName(hook)));
        planDefinition.addExtension(
                CRMI_EFFECTIVE_DATA_REQUIREMENTS,
                new CanonicalType("http://hl7.org/fhir/uv/crmi/Library/moduledefinition-example"));
        planDefinition.setId("ModuleDefinitionTest");
        planDefinition.setUrl("http://test.com/fhir/PlanDefinition/ModuleDefinitionTest");
        repository.update(planDefinition);
        repository.update(ClasspathUtil.loadResource(fhirContext, Library.class, "ModuleDefinitionExample.json"));
        var planDefAdapter = adapterFactory.createPlanDefinition(planDefinition);
        var prefetchList =
                new CrDiscoveryService(planDefinition.getIdElement(), repository).getPrefetchUrlList(planDefAdapter);
        var response = getCdsServiceJson(planDefAdapter, prefetchList);
        assertNotNull(response);
        assertEquals(title, response.getTitle());
        assertEquals("ModuleDefinitionTest", response.getId());
        assertEquals(hook, response.getHook());
        assertEquals(3, response.getPrefetch().size());
    }
}
