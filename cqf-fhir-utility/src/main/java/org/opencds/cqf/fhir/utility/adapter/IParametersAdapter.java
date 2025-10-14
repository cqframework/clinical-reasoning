package org.opencds.cqf.fhir.utility.adapter;

import java.util.List;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IParametersAdapter extends IResourceAdapter {

    boolean hasParameter();

    List<IParametersParameterComponentAdapter> getParameter();

    boolean hasParameter(String name);

    IParametersParameterComponentAdapter getParameter(String name);

    <T extends IBaseDatatype> List<T> getParameterValues(String name);

    void setParameter(List<IBaseBackboneElement> parametersParameterComponents);

    void addParameter(IBase parameter);

    void addParameter(String name, String value);

    void setParameter(String name, int value);

    void addParameter(String name, IBase value);

    void addParameter(String name, IBaseResource resource);

    IBaseBackboneElement addParameter();
}
