package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.ICompositeType;

public interface ITriggerDefinitionAdapter extends IAdapter<ICompositeType> {
    String getName();

    String getType();
}
