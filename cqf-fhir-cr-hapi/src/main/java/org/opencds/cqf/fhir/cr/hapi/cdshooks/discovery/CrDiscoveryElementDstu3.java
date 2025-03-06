package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.hapi.fhir.cdshooks.api.CdsResolutionStrategyEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.PlanDefinition;
import org.hl7.fhir.r4.model.TriggerDefinition;

public class CrDiscoveryElementDstu3 implements ICrDiscoveryElement {
    protected PlanDefinition planDefinition;
    protected PrefetchUrlList prefetchUrlList;

    public CrDiscoveryElementDstu3(PlanDefinition planDefinition, PrefetchUrlList prefetchUrlList) {
        this.planDefinition = planDefinition;
        this.prefetchUrlList = prefetchUrlList;
    }

    public CdsServiceJson getCdsServiceJson() {
        if (planDefinition == null
                || !planDefinition.hasAction()
                || planDefinition.getAction().stream().noneMatch(a -> a.hasTriggerDefinition())) {
            return null;
        }

        var triggerDefs = planDefinition.getAction().stream()
                .filter(a -> a.hasTriggerDefinition())
                .flatMap(a -> a.getTriggerDefinition().stream())
                .filter(t -> t.getType().equals(TriggerDefinition.TriggerType.NAMEDEVENT))
                .collect(Collectors.toList());
        if (triggerDefs == null || triggerDefs.isEmpty()) {
            return null;
        }

        var service = new CdsServiceJson()
                .setId(planDefinition.getIdElement().getIdPart())
                .setTitle(planDefinition.getTitle())
                .setDescription(planDefinition.getDescription())
                .setHook(triggerDefs.get(0).getEventName());

        if (prefetchUrlList == null) {
            prefetchUrlList = new PrefetchUrlList();
        }

        int itemNo = 0;
        if (!prefetchUrlList.stream()
                .anyMatch(p -> p.equals("Patient/{{context.patientId}}")
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
