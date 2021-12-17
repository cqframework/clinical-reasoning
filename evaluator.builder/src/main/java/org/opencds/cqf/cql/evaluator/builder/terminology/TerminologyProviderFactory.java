package org.opencds.cqf.cql.evaluator.builder.terminology;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.engine.terminology.TerminologyProvider;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.engine.terminology.BundleTerminologyProvider;

import ca.uhn.fhir.context.FhirContext;

@Named
public class TerminologyProviderFactory implements org.opencds.cqf.cql.evaluator.builder.TerminologyProviderFactory {

    private Set<TypedTerminologyProviderFactory> terminologyProviderFactories;
    private FhirContext fhirContext;

    @Inject
    public TerminologyProviderFactory(FhirContext fhirContext, Set<TypedTerminologyProviderFactory> terminologyProviderFactories) {
        this.terminologyProviderFactories = terminologyProviderFactories;
        this.fhirContext = fhirContext;
    }

    public TerminologyProvider create(EndpointInfo endpointInfo) {
        if (endpointInfo == null) {
            return null;
        }

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
        requireNonNull(url, "url can not be null");
        requireNonNull(connectionType, "connectionType can not be null");

        for (TypedTerminologyProviderFactory factory : this.terminologyProviderFactories) {
            if (factory.getType().equals(connectionType.getCode())) {
                return factory.create(url, headers);
            }
        }

        throw new IllegalArgumentException("unsupported or unknown connectionType for loading FHIR terminology");
    }

    @Override
    public TerminologyProvider create(IBaseBundle terminologyBundle) {
        if (terminologyBundle == null) {
            return null;
        }

        return new BundleTerminologyProvider(this.fhirContext, terminologyBundle);
    }
}