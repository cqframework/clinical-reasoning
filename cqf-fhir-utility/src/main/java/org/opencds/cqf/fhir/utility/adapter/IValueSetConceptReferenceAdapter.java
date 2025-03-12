package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBaseBackboneElement;

public interface IValueSetConceptReferenceAdapter extends IAdapter<IBaseBackboneElement> {
    String getCode();
}
