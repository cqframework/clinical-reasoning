package org.opencds.cqf.fhir.utility.adapter.dstu3;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.TriggerDefinition;
import org.hl7.fhir.instance.model.api.IBase;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter;

public class TriggerDefinitionAdapter extends BaseAdapter implements ITriggerDefinitionAdapter {

    private final TriggerDefinition triggerDefinition;

    public TriggerDefinitionAdapter(IBase triggerDefinition) {
        super(FhirVersionEnum.DSTU3, triggerDefinition);
        if (!(triggerDefinition instanceof TriggerDefinition)) {
            throw new IllegalArgumentException(
                    "object passed as triggerDefinition argument is not a TriggerDefinition data type");
        }
        this.triggerDefinition = (TriggerDefinition) triggerDefinition;
    }

    @Override
    public TriggerDefinition get() {
        return triggerDefinition;
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
