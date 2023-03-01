package org.opencds.cqf.cql.evaluator.fhir.behavior;

import ca.uhn.fhir.context.FhirContext;

public interface FhirContextUser {
  FhirContext getFhirContext();
}
