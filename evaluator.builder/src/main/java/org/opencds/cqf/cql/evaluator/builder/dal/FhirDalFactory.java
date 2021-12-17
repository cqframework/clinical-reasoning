package org.opencds.cqf.cql.evaluator.builder.dal;

import static org.opencds.cqf.cql.evaluator.builder.util.UriUtil.isFileUri;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.opencds.cqf.cql.evaluator.builder.Constants;
import org.opencds.cqf.cql.evaluator.builder.EndpointInfo;
import org.opencds.cqf.cql.evaluator.fhir.dal.BundleFhirDal;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

import ca.uhn.fhir.context.FhirContext;

@Named
public class FhirDalFactory implements org.opencds.cqf.cql.evaluator.builder.FhirDalFactory {

    private Set<TypedFhirDalFactory> fhirDalFactories;
    private FhirContext fhirContext;

    @Inject
    public FhirDalFactory(FhirContext fhirContext, Set<TypedFhirDalFactory> fhirDalFactories) {
        this.fhirDalFactories = fhirDalFactories;
        this.fhirContext = fhirContext;
    }

    public FhirDal create(EndpointInfo endpointInfo) {
        requireNonNull(endpointInfo, "endpointInfo can not be null");
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

        throw new IllegalArgumentException("invalid connectionType for loading FHIR resources");
    }

    @Override
    public FhirDal create(IBaseBundle resourceBundle) {
        return new BundleFhirDal(this.fhirContext, resourceBundle);
    }
}