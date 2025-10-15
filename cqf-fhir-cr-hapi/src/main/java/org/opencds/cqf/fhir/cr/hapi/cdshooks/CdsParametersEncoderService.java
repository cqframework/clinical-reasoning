package org.opencds.cqf.fhir.cr.hapi.cdshooks;

import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.APPLY_PARAMETER_SUBJECT;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_DRAFT_ORDERS;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_ENCOUNTER_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_PATIENT_ID;
import static org.opencds.cqf.fhir.cr.hapi.cdshooks.CdsCrConstants.CDS_PARAMETER_USER_ID;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import ca.uhn.fhir.rest.api.server.cdshooks.CdsServiceRequestJson;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.BundleHelper;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.VersionUtilities;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;

@SuppressWarnings("UnstableApiUsage")
public class CdsParametersEncoderService {

    private final IAdapterFactory adapterFactory;
    private final IRepository repository;

    public CdsParametersEncoderService(IRepository repository) {
        this.adapterFactory = IAdapterFactory.forFhirContext(repository.fhirContext());
        this.repository = repository;
    }

    public IBaseParameters encodeParams(CdsServiceRequestJson json) {
        var parameters = adapterFactory.createParameters(
                (IBaseParameters) Resources.newBaseForVersion("Parameters", getFhirVersion()));
        parameters.addParameter(APPLY_PARAMETER_SUBJECT, json.getContext().getString(CDS_PARAMETER_PATIENT_ID));
        if (json.getContext().containsKey(CDS_PARAMETER_USER_ID)) {
            parameters.addParameter(
                    APPLY_PARAMETER_PRACTITIONER, json.getContext().getString(CDS_PARAMETER_USER_ID));
        }
        if (json.getContext().containsKey(CDS_PARAMETER_ENCOUNTER_ID)) {
            parameters.addParameter(APPLY_PARAMETER_ENCOUNTER, json.getContext().getString(CDS_PARAMETER_ENCOUNTER_ID));
        }
        var cqlParameters = adapterFactory.createParameters(
                (IBaseParameters) Resources.newBaseForVersion("Parameters", getFhirVersion()));
        if (json.getContext().containsKey(CDS_PARAMETER_DRAFT_ORDERS)) {
            addCqlParameters(
                    cqlParameters,
                    json.getContext().getResource(CDS_PARAMETER_DRAFT_ORDERS),
                    CDS_PARAMETER_DRAFT_ORDERS);
        }
        if (cqlParameters.hasParameter()) {
            parameters.addParameter(APPLY_PARAMETER_PARAMETERS, cqlParameters.get());
        }
        var data = getPrefetchResources(json);
        if (!BundleHelper.getEntry(data).isEmpty()) {
            parameters.addParameter(APPLY_PARAMETER_DATA, data);
        }
        return (IBaseParameters) parameters.get();
    }

    protected void addCqlParameters(IParametersAdapter parameters, IBaseResource contextResource, String paramName) {
        // We are making the assumption that a Library created for a hook will provide parameters for fields
        // specified for the hook
        if (contextResource instanceof IBaseBundle bundle) {
            BundleHelper.getEntryResources(bundle).forEach(r -> parameters.addParameter(paramName, r));
        } else {
            parameters.addParameter(paramName, contextResource);
        }
        if (parameters.getParameter().size() == 1) {
            var listExtension = parameters.getParameter().get(0).get().addExtension();
            listExtension.setUrl(Constants.CPG_PARAMETER_DEFINITION);
            var paramDef = (IBaseDatatype) Resources.newBaseForVersion("ParameterDefinition", getFhirVersion());
            parameters
                    .getModelResolver()
                    .setValue(paramDef, "max", VersionUtilities.stringTypeForVersion(getFhirVersion(), "*"));
            parameters
                    .getModelResolver()
                    .setValue(paramDef, "name", VersionUtilities.codeTypeForVersion(getFhirVersion(), paramName));
            listExtension.setValue(paramDef);
        }
    }

    protected Map<String, IBaseResource> getResourcesFromBundle(IBaseBundle bundle) {
        // using HashMap to avoid duplicates
        Map<String, IBaseResource> resourceMap = new HashMap<>();
        BundleHelper.getEntryResources(bundle)
                .forEach(r -> resourceMap.put(r.fhirType() + r.getIdElement().getIdPart(), r));
        return resourceMap;
    }

    protected IBaseBundle getPrefetchResources(CdsServiceRequestJson json) {
        // using HashMap to avoid duplicates
        Map<String, IBaseResource> resourceMap = new HashMap<>();
        IBaseBundle prefetchResources = BundleHelper.newBundle(getFhirVersion());
        IBaseResource resource;
        for (String key : json.getPrefetchKeys()) {
            resource = json.getPrefetch(key);
            if (resource == null) {
                continue;
            }
            if (resource instanceof IBaseBundle bundle) {
                resourceMap.putAll(getResourcesFromBundle(bundle));
            } else {
                resourceMap.put(resource.fhirType() + resource.getIdElement().getIdPart(), resource);
            }
        }
        resourceMap.forEach(
                (key, value) -> BundleHelper.addEntry(prefetchResources, BundleHelper.newEntryWithResource(value)));
        return prefetchResources;
    }

    protected FhirVersionEnum getFhirVersion() {
        return repository.fhirContext().getVersion().getVersion();
    }
}
