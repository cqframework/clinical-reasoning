package org.opencds.cqf.fhir.cr.questionnaireresponse;

import static java.util.Objects.requireNonNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseReference;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractProcessor;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.ExtractRequest;
import org.opencds.cqf.fhir.cr.questionnaireresponse.extract.IExtractProcessor;
import org.opencds.cqf.fhir.utility.SearchHelper;
import org.opencds.cqf.fhir.utility.monad.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirVersionEnum;

public class QuestionnaireResponseProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseProcessor.class);
    protected final ResourceResolver resourceResolver;
    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected final FhirVersionEnum fhirVersion;
    protected Repository repository;
    protected IExtractProcessor extractProcessor;

    public QuestionnaireResponseProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireResponseProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null);
    }

    public QuestionnaireResponseProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IExtractProcessor extractProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.resourceResolver = new ResourceResolver("QuestionnaireResponse", this.repository);
        this.fhirVersion = repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.extractProcessor = extractProcessor != null ? extractProcessor : new ExtractProcessor(); // repository, modelResolver);
    }
    
    protected <R extends IBaseResource> R resolveQuestionnaireResponse(Either<IIdType, R> questionnaireResponse) {
        return (R) resourceResolver.resolve(questionnaireResponse);
    }

    @SuppressWarnings("unchecked")
    protected IBaseResource resolveQuestionnaire(IBaseResource questionnaireResponse) {
        if (questionnaireResponse.getStructureFhirVersionEnum().equals(FhirVersionEnum.DSTU3)) {
            // IPrimitiveType<String> canonical = null;
            // var pathResult = modelResolver.resolvePath(questionnaireResponse, "questionnaire");
            // canonical = ((IBaseReference) pathResult).getReferenceElement()
            return null;
        } else {
            var canonical = (IPrimitiveType<String>) modelResolver.resolvePath(questionnaireResponse, "questionnaire");
            try {
                return SearchHelper.searchRepositoryByCanonical(repository, canonical, repository.fhirContext().getResourceDefinition("questionnaire").getImplementingClass());
            } catch (Exception e) {
                logger.error(e.getMessage());
                return null;
            }
        }
    }

    public <R extends IBaseResource> IBaseBundle extract(Either<IIdType, R> resource) {
        return extract(resource, null, null);
    }

    public <R extends IBaseResource> IBaseBundle extract(Either<IIdType, R> resource, IBaseParameters parameters, IBaseBundle bundle) {
        return extract(resource, parameters, bundle, new LibraryEngine(repository, evaluationSettings));
    }

    public <R extends IBaseResource> IBaseBundle extract(Either<IIdType, R> resource, IBaseParameters parameters, IBaseBundle bundle, LibraryEngine libraryEngine) {
        var questionnaireResponse = resolveQuestionnaireResponse(resource);
        var questionnaire = resolveQuestionnaire(questionnaireResponse);
        var subject = (IBaseReference) modelResolver.resolvePath(questionnaireResponse, "subject");
        var request = new ExtractRequest(questionnaireResponse, questionnaire, subject == null ? null : subject.getReferenceElement(), parameters, bundle, libraryEngine, modelResolver, repository.fhirContext());
        return extractProcessor.extract(request);
    }
}
