package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;

public interface ParametersAdapter extends ResourceAdapter {

    public List<? extends IBaseBackboneElement> getParameter();

    public IBaseBackboneElement getParameter(String name);

    public List<? extends IBaseDatatype> getParameterValues(String name);

    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents);

    public void addParameter(IBase parameter);

    public void addParameter(String name, IBase value);

    public IBaseBackboneElement addParameter();
}
