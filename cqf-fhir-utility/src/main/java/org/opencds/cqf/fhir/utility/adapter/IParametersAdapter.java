package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IParametersAdapter extends IResourceAdapter {

    public <T extends IBaseBackboneElement> List<T> getParameter();

    public IBaseBackboneElement getParameter(String name);

    public <T extends IBaseDatatype> List<T> getParameterValues(String name);

    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents);

    public void addParameter(IBase parameter);

    public void addParameter(String name, IBase value);

    public void addParameter(String name, IBaseResource resource);

    public IBaseBackboneElement addParameter();
}
