package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

/**
 * This class mainly define encoding capabilities relevant to clinical decision support.
 * It is expected that a new instance of this service will be created to handle each hook invocation
 * by a Bean implementing ICdsCrServiceFactory. See {@code CrCdsHookConfig}.
 */
@SuppressWarnings("squid:S125")
public class CdsCrService implements ICdsCrService {
    protected final RequestDetails requestDetails;
    protected final IRepository repository;

    protected CdsResponseEncoderService cdsResponseEncoderService;
    protected CdsParametersEncoderService cdsParametersEncoderService;

    public CdsCrService(RequestDetails requestDetails, IRepository repository) {
        this.requestDetails = requestDetails;
        this.repository = repository;

        IAdapterFactory iAdapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());

        cdsResponseEncoderService = new CdsResponseEncoderService(repository, iAdapterFactory);
        cdsParametersEncoderService = new CdsParametersEncoderService(repository, iAdapterFactory);
    }

    public CdsCrService(
            RequestDetails requestDetails,
            IRepository repository,
            CdsResponseEncoderService cdsResponseEncoderService,
            CdsParametersEncoderService cdsParametersEncoderService) {

        this.requestDetails = requestDetails;
        this.repository = repository;
        this.cdsResponseEncoderService = cdsResponseEncoderService;
        this.cdsParametersEncoderService = cdsParametersEncoderService;
    }

    public IRepository getRepository() {
        return repository;
    }

    public FhirVersionEnum getFhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }

    public IBaseParameters encodeParams(CdsServiceRequestJson json) {
        return cdsParametersEncoderService.encodeParams(json);
    }

    @SuppressWarnings("unchecked")
    public CdsServiceResponseJson encodeResponse(Object response) {
        return cdsResponseEncoderService.encodeResponse(response);
    }
}
