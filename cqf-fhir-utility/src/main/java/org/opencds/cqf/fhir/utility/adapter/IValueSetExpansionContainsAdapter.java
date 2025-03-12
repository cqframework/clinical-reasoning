package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public interface IValueSetExpansionContainsAdapter extends IAdapter<IBaseBackboneElement> {
    String getCode();

    String getSystem();
}
