package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IParametersParameterComponentAdapter extends IAdapter<IBase> {

    IBaseBackboneElement get();

    String getName();

    void setName(String name);

    List<IParametersParameterComponentAdapter> getPart();

    List<IBaseDatatype> getPartValues(String name);

    void setPart(List<IBaseBackboneElement> parametersParameterComponents);

    IBaseBackboneElement addPart();

    boolean hasPart();

    boolean hasResource();

    IBaseResource getResource();

    void setResource(IBaseResource resource);

    boolean hasValue();

    boolean hasPrimitiveValue();

    String getPrimitiveValue();

    void setValue(IBaseDatatype value);

    IBaseDatatype getValue();
}
