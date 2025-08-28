package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractRequest;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.IExtractProcessor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionnaireResponseProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseProcessor.class);
    protected static final String QUESTIONNAIRE = "Questionnaire";
    protected final ResourceResolver questionnaireResponseResolver;
    protected final ResourceResolver questionnaireResolver;
    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected final FhirVersionEnum fhirVersion;
    protected IRepository repository;
    protected NpmPackageLoader npmPackageLoader;
    protected IExtractProcessor extractProcessor;

    public QuestionnaireResponseProcessor(IRepository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireResponseProcessor(IRepository repository, EvaluationSettings evaluationSettings) {
        this(repository, NpmPackageLoader.DEFAULT, evaluationSettings, null);
    }

    public QuestionnaireResponseProcessor(
            IRepository repository,
            NpmPackageLoader npmPackageLoader,
            EvaluationSettings evaluationSettings,
            IExtractProcessor extractProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.npmPackageLoader = requireNonNull(npmPackageLoader, "npmPackageLoader can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.questionnaireResponseResolver = new ResourceResolver("QuestionnaireResponse", this.repository);
        this.questionnaireResolver = new ResourceResolver(QUESTIONNAIRE, this.repository);
        this.fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.extractProcessor = extractProcessor;
    }

    public FhirContext fhirContext() {
        return repository.fhirContext();
    }

    protected <R extends IBaseResource> R resolveQuestionnaireResponse(Either<IIdType, R> questionnaireResponse) {
        return questionnaireResponseResolver.resolve(questionnaireResponse);
    }

    @SuppressWarnings("unchecked")
    protected <R extends IBaseResource> IBaseResource resolveQuestionnaire(
            IBaseResource questionnaireResponse, Either<IIdType, R> questionnaireId) {
        if (questionnaireId != null) {
            return questionnaireResolver.resolve(questionnaireId);
        } else {
            try {
                IPrimitiveType<String> canonical;
                if (questionnaireResponse.getStructureFhirVersionEnum().equals(FhirVersionEnum.DSTU3)) {
                    var pathResult = modelResolver.resolvePath(questionnaireResponse, "questionnaire");
                    canonical = pathResult == null ? null : ((IBaseReference) pathResult).getReferenceElement();
                } else {
                    canonical =
                            (IPrimitiveType<String>) modelResolver.resolvePath(questionnaireResponse, "questionnaire");
                }
                if (canonical == null) {
                    return null;
                }
                IBaseResource questionnaire = null;
                questionnaire = getQuestionnaireFromContained(questionnaireResponse, canonical, questionnaire);
                if (questionnaire == null) {
                    questionnaire = SearchHelper.searchRepositoryByCanonical(
                            repository,
                            canonical,
                            repository
                                    .fhirContext()
                                    .getResourceDefinition(QUESTIONNAIRE)
                                    .getImplementingClass());
                }
                return questionnaire;
            } catch (Exception e) {
                logger.error(e.getMessage());
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private IBaseResource getQuestionnaireFromContained(
            IBaseResource questionnaireResponse, IPrimitiveType<String> canonical, IBaseResource questionnaire) {
        var contained = (List<IBaseResource>) modelResolver.resolvePath(questionnaireResponse, "contained");
        if (contained != null && !contained.isEmpty()) {
            questionnaire = contained.stream()
                    .filter(r -> r.fhirType().equals(QUESTIONNAIRE)
                            && canonical
                                    .getValueAsString()
                                    .equals(getContainedId(r.getIdElement().getIdPart())))
                    .findFirst()
                    .orElse(null);
        }
        return questionnaire;
    }

    private String getContainedId(String id) {
        return id.startsWith("#") ? id : "#" + id;
    }

    public <R extends IBaseResource> IBaseBundle extract(Either<IIdType, R> resource) {
        return extract(resource, null, null, null, true);
    }

    public <R extends IBaseResource> IBaseBundle extract(
            Either<IIdType, R> questionnaireResponseId,
            Either<IIdType, R> questionnaireId,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData) {
        repository = proxy(repository, useServerData, (IRepository) null, null, null);
        return extract(
                questionnaireResponseId,
                questionnaireId,
                parameters,
                data,
                new LibraryEngine(repository, npmPackageLoader, evaluationSettings));
    }

    public <R extends IBaseResource> IBaseBundle extract(
            Either<IIdType, R> questionnaireResponseId,
            Either<IIdType, R> questionnaireId,
            IBaseParameters parameters,
            IBaseBundle data,
            LibraryEngine libraryEngine) {
        var questionnaireResponse = resolveQuestionnaireResponse(questionnaireResponseId);
        var questionnaire = resolveQuestionnaire(questionnaireResponse, questionnaireId);
        var subject = (IBaseReference) modelResolver.resolvePath(questionnaireResponse, "subject");
        var request = new ExtractRequest(
                questionnaireResponse,
                questionnaire,
                subject == null ? null : subject.getReferenceElement(),
                parameters,
                data,
                libraryEngine,
                modelResolver,
                null);
        var processor = extractProcessor != null ? extractProcessor : new ExtractProcessor();
        return processor.extract(request);
    }
}
