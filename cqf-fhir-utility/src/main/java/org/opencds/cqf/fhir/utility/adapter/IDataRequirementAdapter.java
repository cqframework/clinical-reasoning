package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;

public interface IDataRequirementAdapter extends IAdapter<IBase> {

    boolean hasId();

    String getId();

    boolean hasType();

    String getType();

    boolean hasCodeFilter();

    List<IDataRequirementCodeFilterAdapter> getCodeFilter();
}
