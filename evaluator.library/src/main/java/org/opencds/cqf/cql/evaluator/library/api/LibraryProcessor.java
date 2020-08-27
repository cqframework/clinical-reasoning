package org.opencds.cqf.cql.evaluator.library.api;

import java.util.Set;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public interface LibraryProcessor {
    IBaseParameters evaluate(
    IIdType id,
    String context, 
    String patientId,  
    String periodStart,
    String periodEnd,
    String productLine,
    IBaseResource terminologyEndpoint,
    IBaseResource dataEndpoint,
    IBaseParameters parameters,
    IBaseBundle additionalData,
    Set<String> expressions);
}