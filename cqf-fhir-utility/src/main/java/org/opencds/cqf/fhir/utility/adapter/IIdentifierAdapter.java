package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;

public interface IIdentifierAdapter extends IAdapter<IBase> {
    String getValue();

    boolean hasValue();

    String getSystem();

    boolean hasSystem();
}
