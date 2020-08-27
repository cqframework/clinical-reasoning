package org.opencds.cqf.cql.evaluator.builder;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;
import static org.opencds.cqf.cql.evaluator.fhir.common.ClientFactory.createClient;
import static org.opencds.cqf.cql.evaluator.fhir.common.DirectoryBundler.bundle;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.api.model.ConnectionType;
import org.opencds.cqf.cql.evaluator.builder.api.model.EndpointInfo;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class TerminologyProviderFactory implements org.opencds.cqf.cql.evaluator.builder.api.TerminologyProviderFactory {

    protected FhirContext fhirContext;

    @Inject
    public TerminologyProviderFactory(FhirContext fhirContext) {
        this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext can not be null");
    }

    public TerminologyProvider create(EndpointInfo endpointInfo) {
        Objects.requireNonNull(endpointInfo, "endpointInfo can not be null");
        if (endpointInfo.getUrl() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null)
        {
            endpointInfo.setType(detectTypeFromUrl(endpointInfo.getUrl()));
        }

        return create(endpointInfo.getUrl(), endpointInfo.getType(), endpointInfo.getHeaders());
    }

    protected ConnectionType detectTypeFromUrl(String url) {
        if (isFileUri(url)) {
            return ConnectionType.HL7_FHIR_FILES;
        }
        else {
            return ConnectionType.HL7_FHIR_REST;
        }
    }

    protected TerminologyProvider create(String url, ConnectionType type, List<String> headers) {
        Objects.requireNonNull(url, "url can not be null");
        Objects.requireNonNull(type, "type can not be null");

        switch (type) {
            case HL7_FHIR_REST:
                return createForUrl(url, headers);
            case HL7_FHIR_FILES:
                return createForPath(url);
            default:
                throw new IllegalArgumentException("invalid connectionType for loading FHIR terminology");
        }
    }

    protected TerminologyProvider createForPath(String path) {
        IBaseBundle bundle = bundle(this.fhirContext, path);
        return new BundleTerminologyProvider(this.fhirContext, bundle);
    }

    protected TerminologyProvider createForUrl(String url, List<String> headers) {
        IGenericClient client = createClient(this.fhirContext, url, headers);

        switch (this.fhirContext.getVersion().getVersion()) {
            case DSTU3:
                return new Dstu3FhirTerminologyProvider(client);
            case R4:
                return new R4FhirTerminologyProvider(client);
            default:
                throw new IllegalArgumentException(String.format("unsupported FHIR version: %s", fhirContext));
        }
    }

    // @Override
    // public TerminologyProvider create(IBaseBundle bundle) {
    //     return new BundleTerminologyProvider(this.fhirContext, bundle);
    // }

}