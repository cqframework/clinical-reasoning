package org.opencds.cqf.cql.evaluator.fhir.r4;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseResource;

public class ParametersParameterComponentAdapter
        implements org.opencds.cqf.cql.evaluator.fhir.api.ParametersParameterComponentAdapter {

    protected ParametersParameterComponent castPpc(IBaseBackboneElement ppc) {
        if (ppc == null) {
            throw new IllegalArgumentException("ppc can not be null");
        }

        if (!ppc.fhirType().equals("ParametersParameterComponent")) {
            throw new IllegalArgumentException("resource passed as ppc argument is not a Ppc resource");
        }

        return (ParametersParameterComponent) ppc;
    }

    @Override
    public String getId(IBaseBackboneElement ppc) {
        return castPpc(ppc).getId();
    }

    @Override
    public void setId(IBaseBackboneElement ppc, String id) {
        castPpc(ppc).setId(id);
    }

    @Override
    public String getName(IBaseBackboneElement ppc) {
        return castPpc(ppc).getName();
    }

    @Override
    public void setName(IBaseBackboneElement ppc, String name) {
        castPpc(ppc).setName(name);
    }

    @Override
    public List<IBaseBackboneElement> getPart(IBaseBackboneElement ppc) {
        return castPpc(ppc).getPart().stream().map(x -> (IBaseBackboneElement) x).collect(Collectors.toList());
    }

    @Override
    public void setPart(IBaseBackboneElement ppc, List<IBaseBackboneElement> parametersParameterComponents) {
        castPpc(ppc).setPart(parametersParameterComponents.stream().map(x -> (ParametersParameterComponent) x)
                .collect(Collectors.toList()));
    }

    @Override
    public IBaseBackboneElement addPart(IBaseBackboneElement ppc) {
        return castPpc(ppc).addPart();
    }

    @Override
    public boolean hasPart(IBaseBackboneElement ppc) {
        return castPpc(ppc).hasPart();
    }

    @Override
    public boolean hasResource(IBaseBackboneElement ppc) {
        return castPpc(ppc).hasResource();
    }

    @Override
    public IBaseResource getResource(IBaseBackboneElement ppc) {
        return castPpc(ppc).getResource();
    }

    @Override
    public void setResource(IBaseBackboneElement ppc, IBaseResource resource) {
        castPpc(ppc).setResource((Resource) resource);
    }

    @Override
    public boolean hasValue(IBaseBackboneElement ppc) {
        return castPpc(ppc).hasValue();
    }

    @Override
    public boolean hasPrimitiveValue(IBaseBackboneElement ppc) {
        return castPpc(ppc).hasPrimitiveValue();
    }

    @Override
    public void setValue(IBaseBackboneElement ppc, IBaseDatatype value) {
        castPpc(ppc).setValue((Type) value);
    }

    @Override
    public IBaseDatatype getValue(IBaseBackboneElement ppc) {
        return castPpc(ppc).getValue();
    }

}