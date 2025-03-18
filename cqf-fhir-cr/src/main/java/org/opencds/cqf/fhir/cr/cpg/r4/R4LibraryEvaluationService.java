package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.npm.R4NpmPackageLoader;
import org.opencds.cqf.fhir.cql.npm.R4NpmResourceInfoForCql;
import org.opencds.cqf.fhir.cr.cpg.CqlExecutionProcessor;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4LibraryEvaluationService {

    protected Repository repository;
    protected EvaluationSettings evaluationSettings;

    public R4LibraryEvaluationService(Repository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.evaluationSettings = evaluationSettings;
    }

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

        if (contentEndpoint != null) {
            repository = Repositories.proxy(repository, true, dataEndpoint, contentEndpoint, terminologyEndpoint);
        }
        var libraryEngine = new LibraryEngine(repository, this.evaluationSettings);
        var library = repository.read(Library.class, id);
        // LUKETODO:  pass a non-empty value?
        var engine = Engines.forRepository(
                repository, evaluationSettings, data, R4NpmPackageLoader.DEFAULT, R4NpmResourceInfoForCql.EMPTY);
        var libraryManager = engine.getEnvironment().getLibraryManager();
        var libraryIdentifier = baseCqlExecutionProcessor.resolveLibraryIdentifier(null, library, libraryManager);

        Set<String> expressionSet = null;
        if (expression != null) {
            expressionSet = new HashSet<>(expression);
        }
        try {
            return (Parameters)
                    libraryEngine.evaluate(libraryIdentifier, subject, parameters, data, null, expressionSet);
        } catch (Exception e) {
            return parameters(part("evaluation error", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("error", e.getMessage(), repository)));
        }
    }
}
