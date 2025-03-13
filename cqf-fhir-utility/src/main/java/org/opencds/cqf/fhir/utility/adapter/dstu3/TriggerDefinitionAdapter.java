package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.TriggerDefinition;
import org.hl7.fhir.instance.model.api.ICompositeType;
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
        fhirContext = FhirContext.forDstu3Cached();
        modelResolver = FhirModelResolverCache.resolverForVersion(FhirVersionEnum.DSTU3);
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
        return get().hasEventName();
    }

    @Override
    public String getName() {
        return get().getEventName();
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
