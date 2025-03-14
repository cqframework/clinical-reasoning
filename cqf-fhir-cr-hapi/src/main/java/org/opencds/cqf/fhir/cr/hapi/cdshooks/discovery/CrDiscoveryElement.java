package org.opencds.cqf.fhir.cr.hapi.cdshooks.discovery;

import ca.uhn.hapi.fhir.cdshooks.api.CdsResolutionStrategyEnum;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceJson;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionAdapter;

public class CrDiscoveryElement {
    protected IPlanDefinitionAdapter planDefinition;
    protected PrefetchUrlList prefetchUrlList;

    public CrDiscoveryElement(IPlanDefinitionAdapter planDefinition, PrefetchUrlList prefetchUrlList) {
        this.planDefinition = planDefinition;
        this.prefetchUrlList = prefetchUrlList;
    }

    public String getKey(int itemNo) {
        return "item" + itemNo;
    }

    public CdsServiceJson getCdsServiceJson() {
        if (planDefinition == null
                || !planDefinition.hasAction()
                || planDefinition.getAction().stream().noneMatch(IPlanDefinitionActionAdapter::hasTrigger)) {
            return null;
        }

        var triggerDefs = planDefinition.getAction().stream()
                .filter(IPlanDefinitionActionAdapter::hasTrigger)
                .flatMap(a -> a.getTrigger().stream())
                .filter(t -> t.getType().equals("named-event"))
                .toList();
        if (triggerDefs.isEmpty()) {
            return null;
        }

        var service = new CdsServiceJson()
                .setId(planDefinition.getId().getIdPart())
                .setTitle(planDefinition.getTitle())
                .setDescription(planDefinition.getDescription())
                .setHook(triggerDefs.get(0).getName());

        if (prefetchUrlList == null) {
            prefetchUrlList = new PrefetchUrlList();
        }

        var itemNo = 0;
        if (prefetchUrlList.stream()
                .noneMatch(p -> p.equals("Patient/{{context.patientId}}")
                        || p.equals("Patient?_id={{context.patientId}}")
                        || p.equals("Patient?_id=Patient/{{context.patientId}}"))) {
            var key = getKey(++itemNo);
            service.addPrefetch(key, "Patient?_id={{context.patientId}}");
            service.addSource(key, CdsResolutionStrategyEnum.FHIR_CLIENT);
        }

        for (var item : prefetchUrlList) {
            var key = getKey(++itemNo);
            service.addPrefetch(key, item);
            service.addSource(key, CdsResolutionStrategyEnum.FHIR_CLIENT);
        }

        return service;
    }
}
