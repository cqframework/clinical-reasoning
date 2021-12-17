package org.opencds.cqf.cql.evaluator.builder;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

public interface FhirDalFactory {
    public FhirDal create(EndpointInfo endpointInfo);
    public FhirDal create(IBaseBundle resourceBundle);
}
