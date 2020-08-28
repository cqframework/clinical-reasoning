package org.opencds.cqf.cql.evaluator.builder;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;
import static org.opencds.cqf.cql.evaluator.fhir.ClientFactory.createClient;
import static org.opencds.cqf.cql.evaluator.fhir.DirectoryBundler.bundle;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.fhir.terminology.Dstu3FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.fhir.terminology.R4FhirTerminologyProvider;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.api.Constants;
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
        if (endpointInfo.getAddress() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null)
        {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        return create(endpointInfo.getAddress(), endpointInfo.getType(), endpointInfo.getHeaders());
    }

    protected IBaseCoding detectType(String url) {
        if (isFileUri(url)) {
            return Constants.HL7_FHIR_FILES_CODE;
        }
        else {
            return Constants.HL7_FHIR_REST_CODE;
        }
    }

    protected TerminologyProvider create(String url, IBaseCoding connectionType, List<String> headers) {
        Objects.requireNonNull(url, "url can not be null");
        Objects.requireNonNull(connectionType, "connectionType can not be null");

        switch (connectionType.getCode()) {
            case Constants.HL7_FHIR_REST:
                return createForUrl(url, headers);
            case Constants.HL7_FHIR_FILES:
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