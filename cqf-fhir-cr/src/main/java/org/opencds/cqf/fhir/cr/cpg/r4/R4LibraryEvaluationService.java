package org.opencds.cqf.fhir.cr.cpg.r4;

import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.hl7.cql.model.NamespaceInfo;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
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

        var libraryEngine = new LibraryEngine(repositoryToUse, this.npmPackageLoader, this.evaluationSettings);
        var library = r4FhirOrNpmResourceProviderToUse.resolveLibraryById(id);
        var engine = Engines.forRepository(repository, evaluationSettings, null, npmPackageLoader);
        var allNamespaceInfos = npmPackageLoader.getAllNamespaceInfos();
        var libraryManager = engine.getEnvironment().getLibraryManager();
        npmPackageLoader.initNamespaceMappings(libraryManager);
        var optNamespace = allNamespaceInfos.stream()
                .filter(namespaceInfo -> namespaceInfo.getUri().equals(getNamespaceUrlFromLibraryUrl(library.getUrl())))
                .findFirst();
        // LUKETODO:  do we need namespaceinfo here?
        var libraryIdentifier = baseCqlExecutionProcessor.resolveLibraryIdentifier(
                optNamespace.orElse(null), null, library, libraryManager);

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

    public Parameters evaluate(
            CanonicalType url,
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

        var libraryEngine = new LibraryEngine(repositoryToUse, this.npmPackageLoader, this.evaluationSettings);
        var library = r4FhirOrNpmResourceProviderToUse.resolveLibraryByUrl(url);
        var engine = Engines.forRepository(repository, evaluationSettings, null, npmPackageLoader);
        var allNamespaceInfos = npmPackageLoader.getAllNamespaceInfos();
        var libraryManager = engine.getEnvironment().getLibraryManager();
        npmPackageLoader.initNamespaceMappings(libraryManager);

        var optNamespace = allNamespaceInfos.stream()
                .filter(namespaceInfo -> namespaceInfo.getUri().equals(getNamespaceUrlFromLibraryUrl(library.getUrl())))
                .findFirst();

        // LUKETODO:  do we need namespaceinfo here?
        var libraryIdentifier = baseCqlExecutionProcessor.resolveLibraryIdentifier(
                optNamespace.orElse(null), null, library, libraryManager);

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

    private String getNamespaceUrlFromLibraryUrl(String url) {
        final String[] splitByLibrary = url.split("/Library/");

        if (splitByLibrary.length != 2) {
            return null;
        }

        return splitByLibrary[0];
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
