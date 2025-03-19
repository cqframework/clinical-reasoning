package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseDatatypeElement;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

public interface IDataRequirementCodeFilterAdapter extends IAdapter<IBaseDatatypeElement> {
    boolean hasCode();

    List<ICodingAdapter> getCode();

    boolean hasPath();

    String getPath();

    boolean hasValueSet();

    IPrimitiveType<String> getValueSet();
}
