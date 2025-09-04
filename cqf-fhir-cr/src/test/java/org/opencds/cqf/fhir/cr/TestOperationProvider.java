package org.opencds.cqf.fhir.cr;

import ca.uhn.fhir.context.FhirContext;
import javax.annotation.Nonnull;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.operations.RepositoryOperationProvider;

public class TestOperationProvider {
    public static RepositoryOperationProvider newProvider(
            FhirContext fhirContext, NpmPackageLoader npmPackageLoader, EvaluationSettings evaluationSettings) {
        return new RepositoryOperationProvider(
                fhirContext,
                newActivityDefinitionProcessorFactory(npmPackageLoader, evaluationSettings),
                null,
                null,
                null);
    }

    @Nonnull
    private static ActivityDefinitionProcessorFactory newActivityDefinitionProcessorFactory(
            NpmPackageLoader npmPackageLoader, EvaluationSettings evaluationSettings) {
        return new ActivityDefinitionProcessorFactory(npmPackageLoader, evaluationSettings);
    }
}
