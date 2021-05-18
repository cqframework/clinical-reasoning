package org.opencds.cqf.cql.evaluator.builder.dal;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

@Named
public class FhirDalFactory implements org.opencds.cqf.cql.evaluator.builder.FhirDalFactory {

    private Set<TypedFhirDalFactory> fhirDalFactories;

    @Inject
    public FhirDalFactory(Set<TypedFhirDalFactory> fhirDalFactories) {
        this.fhirDalFactories = fhirDalFactories;
    }

    public FhirDal create(EndpointInfo endpointInfo) {
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

    protected FhirDal create(IBaseCoding connectionType, String url, List<String> headers) {
        requireNonNull(url, "url can not be null");
        requireNonNull(connectionType, "connectionType can not be null");

        for (TypedFhirDalFactory factory : this.fhirDalFactories) {
            if (factory.getType().equals(connectionType.getCode())) {
                return factory.create(url, headers);
            }
        }

        throw new IllegalArgumentException("unsupported connectionType for loading FHIR resources");
    }
}