package org.opencds.cqf.cql.evaluator.builder.dal;

import java.util.List;

import org.opencds.cqf.cql.evaluator.fhir.dal.FhirDal;

public interface TypedFhirDalFactory {
    public String getType();

    public FhirDal create(String url, List<String> headers);
}
