package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;

public interface ITriggerDefinitionAdapter extends IAdapter<IBase> {

    boolean hasName();

    String getName();

    boolean hasType();

    String getType();
}
