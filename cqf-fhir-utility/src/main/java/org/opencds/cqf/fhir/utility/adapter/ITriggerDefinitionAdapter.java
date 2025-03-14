package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.ICompositeType;

public interface ITriggerDefinitionAdapter extends IAdapter<ICompositeType> {

    boolean hasName();

    String getName();

    boolean hasType();

    String getType();
}
