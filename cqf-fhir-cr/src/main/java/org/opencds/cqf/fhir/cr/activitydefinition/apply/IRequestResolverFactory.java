package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IBaseResource;

@FunctionalInterface
public interface IRequestResolverFactory {
    BaseRequestResourceResolver create(IBaseResource baseActivityDefinition);

    public static IRequestResolverFactory getDefault(FhirVersionEnum fhirVersion) {
        switch (fhirVersion) {
            case DSTU3:
                return new org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.Dstu3ResolverFactory();
            case R4:
                return new org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.R4ResolverFactory();
            case R5:
                return new org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.R5ResolverFactory();
            default:
                throw new InvalidRequestException(
                        "No default resolver factory exists for FHIR version: %s".formatted(fhirVersion));
        }
    }
}
