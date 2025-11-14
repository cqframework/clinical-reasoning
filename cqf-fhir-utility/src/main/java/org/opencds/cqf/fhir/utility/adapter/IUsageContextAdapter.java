package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;

public interface IUsageContextAdapter extends IAdapter<IBase> {
    boolean hasCode();

    ICodingAdapter getCode();

    IUsageContextAdapter setCode(ICodingAdapter code);

    boolean hasValue();

    boolean hasValueCodeableConcept();

    ICodeableConceptAdapter getValueCodeableConcept();

    boolean equalsDeep(IUsageContextAdapter other);
}
