package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.PlanDefinition.PlanDefinitionActionComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class PlanDefinitionActionAdapter implements IPlanDefinitionActionAdapter {

    private final PlanDefinitionActionComponent action;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public PlanDefinitionActionAdapter(IBaseBackboneElement action) {
        if (!(action instanceof PlanDefinitionActionComponent)) {
            throw new IllegalArgumentException(
                    "object passed as action argument is not a PlanDefinitionActionComponent data type");
        }
        this.action = (PlanDefinitionActionComponent) action;
        fhirContext = FhirContext.forR4Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R4);
    }

    @Override
    public PlanDefinitionActionComponent get() {
        return action;
    }

    @Override
    public FhirContext fhirContext() {
        return fhirContext;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public boolean hasTrigger() {
        return get().hasTriggerDefinition();
    }

    @Override
    public List<ITriggerDefinitionAdapter> getTrigger() {
        return get().getTriggerDefinition().stream()
                .map(TriggerDefinitionAdapter::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getTriggerType() {
        return List.of();
    }
}
