package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import ca.uhn.hapi.fhir.cdshooks.api.json.CdsServiceResponseJson;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

@SuppressWarnings("squid:S125")
public class CdsCrService implements ICdsCrService {
    protected final IRepository repository;
    protected final IAdapterFactory adapterFactory;
    private final CdsResponseEncoderService cdsResponseEncoderService;
    private final CdsParametersEncoderService cdsParametersEncoderService;

    public CdsCrService(IRepository repository,
        IAdapterFactory adapterFactory,
        CdsResponseEncoderService cdsResponseEncoderService,
        CdsParametersEncoderService cdsParametersEncoderService) {

        this.repository = repository;
        this.adapterFactory = adapterFactory;
        this.cdsResponseEncoderService = cdsResponseEncoderService;
        this.cdsParametersEncoderService = cdsParametersEncoderService;
    }

    public IRepository getRepository() {
        return repository;
    }

    public FhirVersionEnum getFhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }

    public IBaseParameters encodeParams(CdsServiceRequestJson json, RequestDetails requestDetails) {
        return cdsParametersEncoderService.encodeParams(json, requestDetails);


    }

    @SuppressWarnings("unchecked")
    public CdsServiceResponseJson encodeResponse(Object response, RequestDetails requestDetails) {
        return cdsResponseEncoderService.encodeResponse(response, requestDetails);
    }

}
