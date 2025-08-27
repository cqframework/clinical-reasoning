package org.opencds.cqf.fhir.cql.npm;

import ca.uhn.fhir.repository.IRepository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;

// LUKETODO: new name
// LUKETODO: javadoc
public interface FhirOrNpmThingee {

    IRepository getRepository();

    NpmPackageLoader getNpmPackageLoader();

    EvaluationSettings getEvaluationSettings();
}
