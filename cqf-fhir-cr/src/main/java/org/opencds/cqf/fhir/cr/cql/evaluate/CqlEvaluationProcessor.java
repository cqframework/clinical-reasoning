package org.opencds.cqf.fhir.cr.cql.evaluate;

import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;

import ca.uhn.fhir.repository.IRepository;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.cqframework.cql.cql2elm.CqlTranslator;
import org.cqframework.cql.cql2elm.LibraryManager;
import org.cqframework.cql.cql2elm.StringLibrarySourceProvider;
import org.hl7.elm.r1.VersionedIdentifier;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.cql.Engines;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;

@SuppressWarnings("UnstableApiUsage")
public class CqlEvaluationProcessor implements ICqlEvaluationProcessor {
    protected IRepository repository;
    protected EvaluationSettings evaluationSettings;

    public CqlEvaluationProcessor(IRepository repository, EvaluationSettings evaluationSettings) {
        this.repository = repository;
        this.evaluationSettings = evaluationSettings;
    }

    public IBaseParameters evaluate(CqlEvaluationRequest request) {
        try {
            if (StringUtils.isBlank(request.getContent())) {
                return request.getLibraryEngine()
                        .evaluateExpression(
                                request.getExpression(),
                                request.getParameters(),
                                null,
                                request.getSubject(),
                                request.getReferencedLibraries(),
                                request.getData(),
                                request.getContextVariable(),
                                request.getResourceVariable());
            }

            var engine = Engines.forRepository(repository, evaluationSettings, null);
            var libraryManager = engine.getEnvironment().getLibraryManager();
            var libraryIdentifier = resolveLibraryIdentifier(request.getContent(), null, libraryManager);

            request.getLibraryEngine()
                    .getSettings()
                    .getLibrarySourceProviders()
                    .add(new StringLibrarySourceProvider(List.of(request.getContent())));

            return request.getLibraryEngine()
                    .evaluate(
                            libraryIdentifier,
                            request.getSubject(),
                            request.getParameters(),
                            null,
                            request.getData(),
                            null,
                            request.getExpression() == null ? null : Collections.singleton(request.getExpression()));
        } catch (Exception e) {
            request.logException(e.getMessage());
            return newParameters(
                    repository.fhirContext(),
                    newPart(repository.fhirContext(), "evaluation error", request.getOperationOutcome()));
        }
    }

    public VersionedIdentifier resolveLibraryIdentifier(
            String content, IBaseResource library, LibraryManager libraryManager) {

        if (!StringUtils.isBlank(content)) {
            var translatedLibrary =
                    CqlTranslator.fromText(content, libraryManager).getTranslatedLibrary();
            return translatedLibrary == null
                    ? null
                    : new VersionedIdentifier()
                            .withId(
                                    translatedLibrary.getIdentifier() == null
                                            ? null
                                            : translatedLibrary.getIdentifier().getId())
                            .withVersion(
                                    translatedLibrary.getIdentifier() == null
                                            ? null
                                            : translatedLibrary.getIdentifier().getVersion());
        } else if (library == null) {
            return null;
        } else {
            var libraryAdapter = (ILibraryAdapter) IAdapterFactory.createAdapterForResource(library);
            return new VersionedIdentifier()
                    .withId(
                            libraryAdapter.hasUrl()
                                    ? Canonicals.getIdPart(libraryAdapter.getUrl())
                                    : libraryAdapter.hasName() ? libraryAdapter.getName() : null)
                    .withVersion(
                            libraryAdapter.hasVersion()
                                    ? libraryAdapter.getVersion()
                                    : libraryAdapter.hasUrl() ? Canonicals.getVersion(libraryAdapter.getUrl()) : null);
        }
    }
}
