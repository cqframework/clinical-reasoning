package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;

public interface IValueSetConceptReferenceAdapter extends IAdapter<IBase> {

    boolean hasCode();

    String getCode();

    String getDisplay();
}
