package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.hapi.fhir.cdshooks.api.CdsResolutionStrategyEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import org.hl7.fhir.r5.model.PlanDefinition;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.r5.model.TriggerDefinition;

public class CrDiscoveryElementR5 implements ICrDiscoveryElement {
    protected PlanDefinition planDefinition;
    protected PrefetchUrlList prefetchUrlList;

    public CrDiscoveryElementR5(PlanDefinition planDefinition, PrefetchUrlList prefetchUrlList) {
        this.planDefinition = planDefinition;
        this.prefetchUrlList = prefetchUrlList;
    }

    public CdsServiceJson getCdsServiceJson() {
        if (planDefinition == null
                || !planDefinition.hasAction()
                || planDefinition.getAction().stream().noneMatch(PlanDefinitionActionComponent::hasTrigger)) {
            return null;
        }

        var triggerDefs = planDefinition.getAction().stream()
                .filter(PlanDefinitionActionComponent::hasTrigger)
                .flatMap(a -> a.getTrigger().stream())
                .filter(t -> t.getType().equals(TriggerDefinition.TriggerType.NAMEDEVENT))
                .toList();
        if (triggerDefs.isEmpty()) {
            return null;
        }

        var service = new CdsServiceJson()
                .setId(planDefinition.getIdElement().getIdPart())
                .setTitle(planDefinition.getTitle())
                .setDescription(planDefinition.getDescription())
                .setHook(triggerDefs.get(0).getName());

        if (prefetchUrlList == null) {
            prefetchUrlList = new PrefetchUrlList();
        }

        int itemNo = 0;
        if (prefetchUrlList.stream()
                .noneMatch(p -> p.equals("Patient/{{context.patientId}}")
                        || p.equals("Patient?_id={{context.patientId}}")
                        || p.equals("Patient?_id=Patient/{{context.patientId}}"))) {
            String key = getKey(++itemNo);
            service.addPrefetch(key, "Patient?_id={{context.patientId}}");
            service.addSource(key, CdsResolutionStrategyEnum.FHIR_CLIENT);
        }

        for (String item : prefetchUrlList) {
            String key = getKey(++itemNo);
            service.addPrefetch(key, item);
            service.addSource(key, CdsResolutionStrategyEnum.FHIR_CLIENT);
        }

        return service;
    }
}
