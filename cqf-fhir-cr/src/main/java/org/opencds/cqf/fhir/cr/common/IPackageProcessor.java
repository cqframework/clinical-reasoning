package org.opencds.cqf.fhir.cr.common;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;

public interface IPackageProcessor {
    IBaseBundle packageResource(IBaseResource resource);

    IBaseBundle packageResource(IBaseResource resource, String method);
}
