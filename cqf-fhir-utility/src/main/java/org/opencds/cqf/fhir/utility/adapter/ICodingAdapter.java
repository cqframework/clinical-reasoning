package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

public interface ICodingAdapter extends IAdapter<IBase> {
    String getCode();

    IBaseDatatype getCodeType();

    boolean hasCode();

    ICodingAdapter setCode(String code);

    String getDisplay();

    boolean hasDisplay();

    ICodingAdapter setDisplay(String display);

    String getSystem();

    boolean hasSystem();

    ICodingAdapter setSystem(String system);
}
