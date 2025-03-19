package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public interface IPlanDefinitionActionAdapter extends IAdapter<IBaseBackboneElement> {
    boolean hasTrigger();

    List<ITriggerDefinitionAdapter> getTrigger();

    List<String> getTriggerType();
}
