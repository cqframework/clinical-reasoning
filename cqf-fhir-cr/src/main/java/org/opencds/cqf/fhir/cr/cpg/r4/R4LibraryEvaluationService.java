package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.repository.IRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.cpg.CqlExecutionProcessor;
import org.opencds.cqf.fhir.cr.measure.r4.npm.R4FhirOrNpmResourceProvider;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.Repositories;

@SuppressWarnings("squid:S107")
public class R4LibraryEvaluationService {

    protected final IRepository repository;
    protected final NpmPackageLoader npmPackageLoader;
    protected final R4FhirOrNpmResourceProvider r4FhirOrNpmResourceProvider;
    protected final EvaluationSettings evaluationSettings;

    public R4LibraryEvaluationService(
            IRepository repository,
            NpmPackageLoader npmPackageLoader,
            R4FhirOrNpmResourceProvider r4FhirOrNpmResourceProvider,
            EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.npmPackageLoader = npmPackageLoader;
        this.r4FhirOrNpmResourceProvider = r4FhirOrNpmResourceProvider;
        this.evaluationSettings = evaluationSettings;
    }

    // LUKETODO:  how do we handle measure URLs in the context of NPM
    public Parameters evaluate(
            IdType id,
            String subject,
            List<String> expression,
            Parameters parameters,
            Bundle data,
            List<Parameters> prefetchData,
            Endpoint dataEndpoint,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint) {

        var baseCqlExecutionProcessor = new CqlExecutionProcessor();

        if (prefetchData != null) {
            return parameters(part("invalid parameters", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("warning", "prefetchData is not yet supported", repository)));
        }

        final IRepository repositoryToUse;
        final R4FhirOrNpmResourceProvider r4FhirOrNpmResourceProviderToUse;
        if (contentEndpoint != null) {
            repositoryToUse = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
            r4FhirOrNpmResourceProviderToUse = r4FhirOrNpmResourceProvider.withRepositoryIfNonNpm(repositoryToUse);
        } else {
            repositoryToUse = repository;
            r4FhirOrNpmResourceProviderToUse = r4FhirOrNpmResourceProvider;
        }

        var libraryEngine = new LibraryEngine(repositoryToUse, this.evaluationSettings);
        var library = r4FhirOrNpmResourceProviderToUse.resolveLibraryById(id);
        var engine = Engines.forRepository(repository, evaluationSettings, null, npmPackageLoader);
        var libraryManager = engine.getEnvironment().getLibraryManager();
        var libraryIdentifier = baseCqlExecutionProcessor.resolveLibraryIdentifier(null, library, libraryManager);

        Set<String> expressionSet = null;
        if (expression != null) {
            expressionSet = new HashSet<>(expression);
        }
        try {
            return (Parameters)
                    libraryEngine.evaluate(libraryIdentifier, subject, parameters, null, data, null, expressionSet);
        } catch (Exception e) {
            return parameters(part("evaluation error", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("error", e.getMessage(), repository)));
        }
    }
}
