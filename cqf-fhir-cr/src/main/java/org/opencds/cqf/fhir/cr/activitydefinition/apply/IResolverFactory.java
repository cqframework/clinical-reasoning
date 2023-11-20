package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import org.hl7.fhir.instance.model.api.IBaseResource;

@FunctionalInterface
public interface IResolverFactory {
    BaseRequestResourceResolver create(IBaseResource baseActivityDefinition);
}
