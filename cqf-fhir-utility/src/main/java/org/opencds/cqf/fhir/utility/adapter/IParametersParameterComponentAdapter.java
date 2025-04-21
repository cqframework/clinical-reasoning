package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IParametersParameterComponentAdapter extends IAdapter<IBaseBackboneElement> {

    public IBaseBackboneElement get();

    public String getName();

    public void setName(String name);

    public List<IParametersParameterComponentAdapter> getPart();

    public List<IBaseDatatype> getPartValues(String name);

    public void setPart(List<IBaseBackboneElement> parametersParameterComponents);

    public IBaseBackboneElement addPart();

    public boolean hasPart();

    public boolean hasResource();

    public IBaseResource getResource();

    public void setResource(IBaseResource resource);

    public boolean hasValue();

    public boolean hasPrimitiveValue();

    public void setValue(IBaseDatatype value);

    public IBaseDatatype getValue();
}
