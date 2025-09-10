package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.cpg.CqlExecutionProcessor;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.opencds.cqf.fhir.utility.repository.Repositories;

@SuppressWarnings("squid:S107")
public class R4CqlExecutionService {

    protected IRepository repository;
    protected NpmPackageLoader npmPackageLoader;
    protected EvaluationSettings evaluationSettings;

    public R4CqlExecutionService(
            IRepository repository, NpmPackageLoader npmPackageLoader, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.npmPackageLoader = npmPackageLoader;
        this.evaluationSettings = evaluationSettings;
    }

    // should use adapters to make this version agnostic
    public Parameters evaluate(
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
            String cqlContent) {

        var baseCqlExecutionProcessor = new CqlExecutionProcessor();

        if (prefetchData != null) {
            return parameters(part("invalid parameters", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("warning", "prefetchData is not yet supported", repository)));
        }

        if (expression == null && cqlContent == null) {
            return parameters(part("invalid parameters", (OperationOutcome) baseCqlExecutionProcessor.createIssue(
                    "error",
                    "The $cql operation requires the expression parameter and/or cqlContent parameter to exist",
                    repository)));
        }

        try {
            if (contentEndpoint != null) {
                repository = Repositories.proxy(
                        repository, useServerData.booleanValue(), dataEndpoint, contentEndpoint, terminologyEndpoint);
            }
            var libraryEngine = new LibraryEngine(this.repository, this.npmPackageLoader, this.evaluationSettings);

            // LUKETODO:  NPM part goes in here:  "also include these libraries" which is what the Map is
            // inject the NpmPackageLoader here
            var libraries = baseCqlExecutionProcessor.resolveIncludedLibraries(library);

            // LUKETODO:  how would we get blank CQL content?
            if (StringUtils.isBlank(cqlContent)) {

                return (Parameters) libraryEngine.evaluateExpression(
                        expression,
                        parameters == null ? new Parameters() : parameters,
                        null,
                        subject,
                        libraries,
                        data,
                        null,
                        null);
            }

            var engine = Engines.forRepository(repository, evaluationSettings, null, npmPackageLoader);
            var libraryManager = engine.getEnvironment().getLibraryManager();

            var namespaceInfo = getNamespaceInfoForCqlContent(cqlContent);
            var libraryIdentifier =
                    baseCqlExecutionProcessor.resolveLibraryIdentifier(namespaceInfo, cqlContent, null, libraryManager);

            return (Parameters) libraryEngine.evaluate(
                    libraryIdentifier,
                    subject,
                    parameters,
                    null,
                    data,
                    null,
                    expression == null ? null : Collections.singleton(expression));

        } catch (Exception e) {
            return parameters(part("evaluation error", (OperationOutcome)
                    baseCqlExecutionProcessor.createIssue("error", e.getMessage(), repository)));
        }
    }

    // LUKETODO:  share with R4LibraryEvaluationService, etc
    private NamespaceInfo getNamespaceInfoForCqlContent(String cqlContent) {
        final String namespaceName = getNamespaceNameFromCqlString(cqlContent);
        if (namespaceName == null) {
            return null;
        }

        return npmPackageLoader.getAllNamespaceInfos().stream()
                .filter(namespaceInfo -> namespaceName.equals(namespaceInfo.getName()))
                .findFirst()
                .orElse(null);
    }

    // LUKETODO:  run this by Brenin because this is gross
    // Really really gross:  Get the namespace name from the library declaration in the CQL string:
    @Nullable
    private String getNamespaceNameFromCqlString(String cqlContent) {
        if (StringUtils.isBlank(cqlContent)) {
            return null;
        }
        final String[] splitByLibrary = cqlContent.split("library ");

        if (splitByLibrary.length < 2) {
            return null;
        }

        final String pastLibrary = splitByLibrary[1];

        // We don't know what kind of whitespace might be after the library name
        final String[] pastLibrarySplitByWhitespace = pastLibrary.split("\\s+");

        if (pastLibrarySplitByWhitespace.length < 1) {
            return null;
        }

        final String libraryIncludingNamespace = pastLibrarySplitByWhitespace[0];

        int lastDotIndex = libraryIncludingNamespace.lastIndexOf('.');

        if (lastDotIndex != -1) {
            return libraryIncludingNamespace.substring(0, lastDotIndex);
        } else {
            return null;
        }
    }
}
