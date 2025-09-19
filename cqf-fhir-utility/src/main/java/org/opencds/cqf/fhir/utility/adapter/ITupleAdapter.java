package org.opencds.cqf.fhir.utility.adapter;

import java.util.LinkedHashMap;
import org.hl7.fhir.instance.model.api.IBase;

public interface ITupleAdapter extends IAdapter<IBase> {

    LinkedHashMap<String, Object> getProperties();
}
