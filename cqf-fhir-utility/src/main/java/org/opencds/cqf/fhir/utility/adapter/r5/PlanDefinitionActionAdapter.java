package org.opencds.cqf.fhir.utility.adapter.r5;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r5.model.PlanDefinition.PlanDefinitionActionComponent;
import org.opencds.cqf.fhir.utility.adapter.BaseAdapter;
import org.opencds.cqf.fhir.utility.adapter.IPlanDefinitionActionAdapter;
import org.opencds.cqf.fhir.utility.adapter.ITriggerDefinitionAdapter;

public class PlanDefinitionActionAdapter extends BaseAdapter implements IPlanDefinitionActionAdapter {

    private final PlanDefinitionActionComponent action;

    public PlanDefinitionActionAdapter(IBase action) {
        super(FhirVersionEnum.R5, action);
        if (!(action instanceof PlanDefinitionActionComponent)) {
            throw new IllegalArgumentException(
                    "object passed as action argument is not a PlanDefinitionActionComponent data type");
        }
        this.action = (PlanDefinitionActionComponent) action;
    }

    @Override
    public PlanDefinitionActionComponent get() {
        return action;
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
        return get().getTrigger().stream().map(t -> t.getType().toCode()).toList();
    }
}
