package org.opencds.cqf.fhir.utility.adapter.r4;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.r4.model.PlanDefinition.PlanDefinitionActionComponent;
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
        return get().hasTrigger();
    }

    @Override
    public List<ITriggerDefinitionAdapter> getTrigger() {
        return get().getTrigger().stream().map(TriggerDefinitionAdapter::new).collect(Collectors.toUnmodifiableList());
    }

    @Override
    public List<String> getTriggerType() {
        return get().getTrigger().stream().map(t -> t.getType().toCode()).collect(Collectors.toUnmodifiableList());
    }
}
