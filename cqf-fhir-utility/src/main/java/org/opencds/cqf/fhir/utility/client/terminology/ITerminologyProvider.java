package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import java.util.Optional;

public interface ITerminologyProvider {

    IBaseResource expand(
        IValueSetAdapter valueSet, IEndpointAdapter endpoint, IParametersAdapter parameters);

    IBaseResource expand(IEndpointAdapter endpoint, IParametersAdapter parameters, FhirVersionEnum fhirVersion);

    IBaseResource expand(
        IEndpointAdapter endpoint,
        IParametersAdapter parameters,
        String url,
        String valueSetVersion,
        FhirVersionEnum fhirVersion);

    IBaseResource expand(IGenericClient fhirClient, String url, IBaseParameters parameters);

    Class<? extends IBaseResource> getCodeSystemClass();

    Optional<IDomainResource> getCodeSystemResource(IEndpointAdapter endpoint, String url);

    Optional<IDomainResource> getLatestValueSetResource(IEndpointAdapter endpoint, String url);

    org.hl7.fhir.r4.model.TerminologyCapabilities getR4TerminologyCapabilities(IEndpointAdapter endpoint);

    Class<? extends IBaseResource> getValueSetClass();

    Optional<IDomainResource> getValueSetResource(IEndpointAdapter endpoint, String url);
}
