package org.opencds.cqf.cql.evaluator.builder.terminology;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

public class TerminologyProviderFactory implements org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory {

    private Set<TypedTerminologyProviderFactory> terminologyProviderFactories;
    private FhirContext fhirContext;

    @Inject
    public TerminologyProviderFactory(FhirContext fhirContent, Set<TypedTerminologyProviderFactory> terminologyProviderFactories) {
        this.terminologyProviderFactories = terminologyProviderFactories;
        this.fhirContext = fhirContent;
    }

    public TerminologyProvider create(EndpointInfo endpointInfo) {
        Objects.requireNonNull(endpointInfo, "endpointInfo can not be null");
        if (endpointInfo.getAddress() == null) {
            throw new IllegalArgumentException("endpointInfo must have a url defined");
        }

        if (endpointInfo.getType() == null) {
            endpointInfo.setType(detectType(endpointInfo.getAddress()));
        }

        return create(endpointInfo.getType(), endpointInfo.getAddress(), endpointInfo.getHeaders());
    }

    protected IBaseCoding detectType(String url) {
        if (isFileUri(url)) {
            return Constants.HL7_FHIR_FILES_CODE;
        } else {
            return Constants.HL7_FHIR_REST_CODE;
        }
    }

    protected TerminologyProvider create(IBaseCoding connectionType, String url, List<String> headers) {
        Objects.requireNonNull(url, "url can not be null");
        Objects.requireNonNull(connectionType, "connectionType can not be null");

        for (TypedTerminologyProviderFactory factory : this.terminologyProviderFactories) {
            if (factory.getType().equals(connectionType.getCode())) {
                return factory.create(url, headers);
            }
        }

        throw new IllegalArgumentException("invalid connectionType for loading FHIR terminology");
    }

    @Override
    public TerminologyProvider create(IBaseBundle terminologyBundle) {
        return new BundleTerminologyProvider(this.fhirContext, terminologyBundle);
    }
}