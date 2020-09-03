package org.opencds.cqf.cql.evaluator.library;

import java.util.Set;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public interface LibraryEvaluator {
    IBaseParameters evaluate(
    IIdType id,
    String context, 
    String patientId,  
    String periodStart,
    String periodEnd,
    String productLine,
    IBaseResource libraryEndpoint,
    IBaseResource terminologyEndpoint,
    IBaseResource dataEndpoint,
    IBaseParameters parameters,
    IBaseBundle additionalData,
    Set<String> expressions);

    IBaseParameters evaluate(
        VersionedIdentifier identifier,
        String context, 
        String patientId,  
        String periodStart,
        String periodEnd,
        String productLine,
        IBaseResource libraryEndpoint,
        IBaseResource terminologyEndpoint,
        IBaseResource dataEndpoint,
        IBaseParameters parameters,
        IBaseBundle additionalData,
        Set<String> expressions);
}