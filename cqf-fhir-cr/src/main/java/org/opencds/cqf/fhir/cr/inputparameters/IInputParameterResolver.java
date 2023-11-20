package org.opencds.cqf.fhir.cr.inputparameters;

import org.hl7.fhir.instance.model.api.IBaseParameters;

public interface IInputParameterResolver {
    public IBaseParameters getParameters();
}
