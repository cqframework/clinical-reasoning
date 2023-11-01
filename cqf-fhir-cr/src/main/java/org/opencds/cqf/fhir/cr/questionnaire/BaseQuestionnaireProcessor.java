package org.opencds.cqf.fhir.cr.questionnaire;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.questionnaire.common.PrePopulateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.Nonnull;

public abstract class BaseQuestionnaireProcessor<T> {
    protected static final Logger logger = LoggerFactory.getLogger(BaseQuestionnaireProcessor.class);

    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected Repository repository;
    protected LibraryEngine libraryEngine;

    protected String patientId;
    protected IBaseParameters parameters;
    protected IBaseBundle bundle;
    protected String libraryUrl;
    protected static final String subjectType = "Patient";

    protected BaseQuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");

        modelResolver = FhirModelResolverCache.resolverForVersion(
                repository.fhirContext().getVersion().getVersion());
    }

    public static <T extends IBase> Optional<T> castOrThrow(IBase obj, Class<T> type, String errorMessage) {
        if (obj == null) return Optional.empty();
        if (type.isInstance(obj)) {
            return Optional.of(type.cast(obj));
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public abstract <CanonicalType extends IPrimitiveType<String>> T resolveQuestionnaire(
            IIdType id, CanonicalType canonical, IBaseResource questionnaire);

    public <CanonicalType extends IPrimitiveType<String>> T prePopulate(
            IIdType id,
            CanonicalType canonical,
            IBaseResource questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        repository = org.opencds.cqf.fhir.utility.repository.Repositories.proxy(
                repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
        return prePopulate(
                resolveQuestionnaire(id, canonical, questionnaire),
                patientId,
                parameters,
                bundle,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <CanonicalType extends IPrimitiveType<String>> T prePopulate(
            IIdType id,
            CanonicalType canonical,
            IBaseResource questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        return prePopulate(
                resolveQuestionnaire(id, canonical, questionnaire), patientId, parameters, bundle, libraryEngine);
    }

    public abstract T prePopulate(
            T questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine);

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource populate(
            IIdType id,
            CanonicalType canonical,
            IBaseResource questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        repository = org.opencds.cqf.fhir.utility.repository.Repositories.proxy(
                repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
        return populate(
                resolveQuestionnaire(id, canonical, questionnaire),
                patientId,
                parameters,
                bundle,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource populate(
            IIdType id,
            CanonicalType canonical,
            IBaseResource questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        return populate(
                resolveQuestionnaire(id, canonical, questionnaire), patientId, parameters, bundle, libraryEngine);
    }

    public abstract IBaseResource populate(
            T questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine);

    public abstract T generateQuestionnaire(String id);

    public abstract IBaseBundle packageQuestionnaire(T questionnaire, boolean isPut);

    public IBaseBundle packageQuestionnaire(T questionnaire) {
        return packageQuestionnaire(questionnaire, false);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            IIdType id, CanonicalType canonical, IBaseResource questionnaire, boolean isPut) {
        return packageQuestionnaire(resolveQuestionnaire(id, canonical, questionnaire), isPut);
    }
}
