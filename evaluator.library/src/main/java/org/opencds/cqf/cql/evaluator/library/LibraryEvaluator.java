package org.opencds.cqf.cql.evaluator.library;

import java.util.Set;

import org.cqframework.cql.elm.execution.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;

public interface LibraryEvaluator {
    /**
     * The function evaluates a FHIR library by Id and returns a Parameters resource
     * that contains the evaluation result
     * 
     * @param id                  the Id of the Library to evaluate
     * @param context             the context of the evaluation (e.g. "Patient",
     *                            "Unspecified")
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param periodStart         the "Measurement Period" start date, if applicable
     * @param periodEnd           the "Measurement Period" end date, if applicable
     * @param productLine         the "Product Line", if applicable
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param parameters          additional Parameters to set for the Library
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of Expressions in the Library to evaluate
     * @return IBaseParameters
     */
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


    /**
     * The function evaluates a CQL / FHIR library by VersionedIdentifier and
     * returns a Parameters resource that contains the evaluation result
     * 
     * @param identifier                  the VersionedIdentifier of the Library to evaluate
     * @param context             the context of the evaluation (e.g. "Patient",
     *                            "Unspecified")
     * @param patientId           the patient Id to use for evaluation, if
     *                            applicable
     * @param periodStart         the "Measurement Period" start date, if applicable
     * @param periodEnd           the "Measurement Period" end date, if applicable
     * @param productLine         the "Product Line", if applicable
     * @param libraryEndpoint     the Endpoint to use for loading Library resources,
     *                            if applicable
     * @param terminologyEndpoint the Endpoint to use for Terminology operations, if
     *                            applicable
     * @param dataEndpoint        the Endpoint to use for data, if applicable
     * @param parameters          additional Parameters to set for the Library
     * @param additionalData      additional data to use during evaluation
     * @param expressions         names of Expressions in the Library to evaluate
     * @return IBaseParameters
     */
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