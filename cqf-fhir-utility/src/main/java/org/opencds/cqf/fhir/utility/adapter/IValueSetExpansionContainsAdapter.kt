package org.opencds.cqf.fhir.utility.adapter;

import org.hl7.fhir.instance.model.api.IBase;

public interface IValueSetExpansionContainsAdapter extends IAdapter<IBase> {

    boolean hasCode();

    String getCode();

    boolean hasSystem();

    String getSystem();

    boolean hasDisplay();

    String getDisplay();
}
