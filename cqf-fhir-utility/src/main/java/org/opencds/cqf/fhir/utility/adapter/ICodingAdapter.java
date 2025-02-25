package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.ICompositeType;

public interface ICodingAdapter extends IAdapter<ICompositeType> {
    String getCode();

    boolean hasCode();

    String getDisplay();

    boolean hasDisplay();

    String getSystem();

    boolean hasSystem();
}
