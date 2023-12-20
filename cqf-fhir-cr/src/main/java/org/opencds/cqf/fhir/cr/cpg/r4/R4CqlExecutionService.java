package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.cpg.BaseCqlExecutionProcessor;
import org.opencds.cqf.fhir.utility.repository.Repositories;

public class R4CqlExecutionService {

    protected Repository repository;
    protected EvaluationSettings evaluationSettings;

    public R4CqlExecutionService(Repository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.evaluationSettings = evaluationSettings;
    }

    public Parameters evaluate(
            // RequestDetails requestDetails,
            String subject,
            String expression,
            Parameters parameters,
            List<Parameters> library,
            BooleanType useServerData,
            Bundle data,
            List<Parameters> prefetchData,
            Endpoint dataEndpoint,
            Endpoint contentEndpoint,
            Endpoint terminologyEndpoint,
            String content) {

        var baseCqlExecutionProcessor = new BaseCqlExecutionProcessor();

        if (prefetchData != null) {
            return parameters(part("invalid parameters", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("warning", "prefetchData is not yet supported", repository)));
        }

        if (expression == null && content == null) {
            return parameters(part("invalid parameters", (OperationOutcome) baseCqlExecutionProcessor.createIssue(
                    "error",
                    "The $cql operation requires the expression parameter and/or content parameter to exist",
                    repository)));
        }

        try {
            if (contentEndpoint != null) {
                repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
            }
            var libraryEngine = new LibraryEngine(repository, this.evaluationSettings);

            var libraries = baseCqlExecutionProcessor.resolveIncludedLibraries(library);

            if (StringUtils.isBlank(content)) {

                return (Parameters) libraryEngine.evaluateExpression(
                        expression, parameters == null ? new Parameters() : parameters, subject, libraries, data);
            }

            var engine = Engines.forRepositoryAndSettings(evaluationSettings, repository, null);
            var libraryManager = engine.getEnvironment().getLibraryManager();
            var libraryIdentifier = baseCqlExecutionProcessor.resolveLibraryIdentifier(content, null, libraryManager);

            return (Parameters) libraryEngine.evaluate(
                    libraryIdentifier,
                    subject,
                    parameters,
                    data,
                    expression == null ? null : Collections.singleton(expression));

        } catch (Exception e) {
            e.printStackTrace();
            return parameters(part("evaluation error", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("error", e.getMessage(), repository)));
        }
    }
}
