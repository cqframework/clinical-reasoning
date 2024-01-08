package org.opencds.cqf.fhir.cr.questionnaire;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirVersionEnum;
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
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IGenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.packages.PackageProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionnaireProcessor {
    protected static final Logger logger = LoggerFactory.getLogger(BaseQuestionnaireProcessor.class);

    protected final ResourceResolver resourceResolver;
    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected final FhirVersionEnum fhirVersion;
    protected Repository repository;
    protected IGenerateProcessor generateProcessor;
    protected IPackageProcessor packageProcessor;
    protected IPopulateProcessor populateProcessor;

    protected String patientId;
    protected IBaseParameters parameters;
    protected IBaseBundle bundle;
    protected String libraryUrl;
    protected static final String SUBJECT_TYPE = "Patient";

    public QuestionnaireProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null, null);
    }

    public QuestionnaireProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IGenerateProcessor generateProcessor,
            IPackageProcessor packageProcessor,
            IPopulateProcessor populateProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.resourceResolver = new ResourceResolver("Questionnaire", this.repository);
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.generateProcessor =
                generateProcessor != null ? generateProcessor : new GenerateProcessor(this.repository, modelResolver);
        this.packageProcessor =
                packageProcessor != null ? packageProcessor : new PackageProcessor(this.repository, modelResolver);
        this.populateProcessor = populateProcessor != null ? populateProcessor : new PopulateProcessor();
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveQuestionnaire(
            Either3<C, IIdType, R> questionnaire) {
        return (R) resourceResolver.resolve(questionnaire);
    }

    public IBaseResource generateQuestionnaire(String id) {
        return generateProcessor.generate(id);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire) {
        return packageQuestionnaire(resolveQuestionnaire(questionnaire));
    }

    public IBaseBundle packageQuestionnaire(IBaseResource questionnaire) {
        return packageProcessor.packageResource(questionnaire);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire, boolean isPut) {
        return packageQuestionnaire(resolveQuestionnaire(questionnaire), isPut);
    }

    public IBaseBundle packageQuestionnaire(IBaseResource questionnaire, boolean isPut) {
        return packageProcessor.packageResource(questionnaire, isPut ? "PUT" : "POST");
    }

    public <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        repository = org.opencds.cqf.fhir.utility.repository.Repositories.proxy(
                repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
        return prePopulate(
                resolveQuestionnaire(questionnaire),
                patientId,
                parameters,
                bundle,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        return prePopulate(resolveQuestionnaire(questionnaire), patientId, parameters, bundle, libraryEngine);
    }

    public <R extends IBaseResource> R prePopulate(
            IBaseResource questionnaire,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        final PopulateRequest populateRequest = new PopulateRequest(
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                bundle,
                libraryEngine,
                modelResolver);
        return prePopulate(populateRequest);
    }

    @SuppressWarnings("unchecked")
    public <R extends IBaseResource> R prePopulate(PopulateRequest request) {
        return (R) populateProcessor.prePopulate(request);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource populate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        repository = org.opencds.cqf.fhir.utility.repository.Repositories.proxy(
                repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
        return populate(
                resolveQuestionnaire(questionnaire),
                patientId,
                parameters,
                bundle,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource populate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        return populate(resolveQuestionnaire(questionnaire), patientId, parameters, bundle, libraryEngine);
    }

    public IBaseResource populate(
            IBaseResource questionnaire,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        final PopulateRequest populateRequest = new PopulateRequest(
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                bundle,
                libraryEngine,
                modelResolver);
        return populate(populateRequest);
    }

    public IBaseResource populate(PopulateRequest request) {
        return populateProcessor.populate(request);
    }
}
