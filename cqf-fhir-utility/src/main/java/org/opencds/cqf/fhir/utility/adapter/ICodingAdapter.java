package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.ICompositeType;

public interface ICodingAdapter extends IAdapter<ICompositeType> {
    String getCode();

    boolean hasCode();

    ICodingAdapter setCode(String code);

    String getDisplay();

    boolean hasDisplay();

    ICodingAdapter setDisplay(String display);

    String getSystem();

    boolean hasSystem();

    ICodingAdapter setSystem(String system);
}
