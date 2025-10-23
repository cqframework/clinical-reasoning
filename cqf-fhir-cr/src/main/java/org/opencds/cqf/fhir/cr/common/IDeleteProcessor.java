package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IDeleteProcessor {

    IBaseBundle deleteResource(IBaseResource resource, IBaseParameters parameters);
}
