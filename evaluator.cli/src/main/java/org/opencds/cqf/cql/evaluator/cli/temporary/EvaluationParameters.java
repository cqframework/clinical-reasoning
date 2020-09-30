package org.opencds.cqf.cql.evaluator.cli.temporary;

import org.apache.commons.lang3.tuple.Pair;

import ca.uhn.fhir.context.FhirVersionEnum;

// WARNING: This class is just a temporary stand-in until the builder is complete.
// We should replace this at the earliest opportunity
// DON'T FIX IT, DON'T EXTEND IT. KILL IT!
public class EvaluationParameters {
    public Pair<String,String> model;
    public String terminologyUrl;
    public String libraryUrl;
    public String libraryName;
    public Pair<String, String> contextParameter;
    public FhirVersionEnum fhirVersion;
}