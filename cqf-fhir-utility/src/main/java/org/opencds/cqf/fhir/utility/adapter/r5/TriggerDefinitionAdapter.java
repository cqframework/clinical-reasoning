package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.r5.model.TriggerDefinition;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class TriggerDefinitionAdapter implements ITriggerDefinitionAdapter {

    private final TriggerDefinition triggerDefinition;
    private final FhirContext fhirContext;
    private final ModelResolver modelResolver;

    public TriggerDefinitionAdapter(ICompositeType triggerDefinition) {
        if (!(triggerDefinition instanceof TriggerDefinition)) {
            throw new IllegalArgumentException(
                    "object passed as triggerDefinition argument is not a TriggerDefinition data type");
        }
        this.triggerDefinition = (TriggerDefinition) triggerDefinition;
        fhirContext = FhirContext.forR5Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.R5);
    }

    @Override
    public TriggerDefinition get() {
        return triggerDefinition;
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
    public boolean hasName() {
        return get().hasName();
    }

    @Override
    public String getName() {
        return get().getName();
    }

    @Override
    public boolean hasType() {
        return get().hasType();
    }

    @Override
    public String getType() {
        return get().getType().toCode();
    }
}
