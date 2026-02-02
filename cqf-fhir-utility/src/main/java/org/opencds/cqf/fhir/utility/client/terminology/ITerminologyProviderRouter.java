package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import java.util.List;
import java.util.Optional;

public interface ITerminologyProviderRouter extends ITerminologyProvider {

    IBaseResource expand(
        IValueSetAdapter valueSet, List<IEndpointAdapter> endpoints, IParametersAdapter parameters);

    IBaseResource expand(List<IEndpointAdapter> endpoints, IParametersAdapter parameters, FhirVersionEnum fhirVersion);

    IBaseResource expand(
        List<IEndpointAdapter> endpoints,
        IParametersAdapter parameters,
        String url,
        String valueSetVersion,
        FhirVersionEnum fhirVersion);

    Optional<IDomainResource> getValueSetResource(List<IEndpointAdapter> endpoints, String url);

    Optional<IDomainResource> getCodeSystemResource(List<IEndpointAdapter> endpoints, String url);

    Optional<IDomainResource> getLatestValueSetResource(List<IEndpointAdapter> endpoints, String url);

    TerminologyServerClientSettings getTerminologyServerClientSettings(IEndpointAdapter endpoints);
}
