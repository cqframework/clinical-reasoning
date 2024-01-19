package org.opencds.cqf.fhir.cr.questionnaire;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

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
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IGenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.packages.PackageProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Either3;

public class QuestionnaireProcessor {
    protected static final String SUBJECT_TYPE = "Patient";

    protected final ResourceResolver questionnaireResolver;
    protected final ResourceResolver structureDefResolver;
    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected final FhirVersionEnum fhirVersion;
    protected Repository repository;
    protected IGenerateProcessor generateProcessor;
    protected IPackageProcessor packageProcessor;
    protected IPopulateProcessor populateProcessor;

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
        this.questionnaireResolver = new ResourceResolver("Questionnaire", this.repository);
        this.structureDefResolver = new ResourceResolver("StructureDefinition", this.repository);
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.generateProcessor = generateProcessor != null ? generateProcessor : new GenerateProcessor(this.repository);
        this.packageProcessor = packageProcessor != null ? packageProcessor : new PackageProcessor(this.repository);
        this.populateProcessor = populateProcessor != null ? populateProcessor : new PopulateProcessor();
    }

    public <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R resolveQuestionnaire(
            Either3<CanonicalType, IIdType, R> questionnaire) {
        return (R) questionnaireResolver.resolve(questionnaire);
    }

    public <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R resolveStructureDefinition(
            Either3<CanonicalType, IIdType, R> structureDef) {
        return (R) structureDefResolver.resolve(structureDef);
    }

    public IBaseResource generateQuestionnaire(String id) {
        return generateProcessor.generate(id);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource generateQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> profile) {
        return generateQuestionnaire(profile, false, true, null, null, null, null, null);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource generateQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> profile, Boolean supportedOnly, Boolean requiredOnly) {
        return generateQuestionnaire(profile, supportedOnly, requiredOnly, null, null, null, null, null);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource generateQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> profile,
            Boolean supportedOnly,
            Boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint,
            String id) {
        return generateQuestionnaire(
                profile,
                supportedOnly,
                requiredOnly,
                subjectId,
                parameters,
                bundle,
                useServerData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint),
                id);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource generateQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> profile,
            Boolean supportedOnly,
            Boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository,
            String id) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return generateQuestionnaire(
                profile,
                supportedOnly,
                requiredOnly,
                subjectId,
                parameters,
                bundle,
                new LibraryEngine(repository, evaluationSettings),
                id);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource generateQuestionnaire(
            Either3<CanonicalType, IIdType, IBaseResource> profile,
            Boolean supportedOnly,
            Boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            String id) {
        var request = new GenerateRequest(
                supportedOnly,
                requiredOnly,
                subjectId == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                bundle,
                libraryEngine == null ? new LibraryEngine(repository, evaluationSettings) : libraryEngine,
                modelResolver);
        return generateQuestionnaire(request, resolveStructureDefinition(profile), id);
    }

    public IBaseResource generateQuestionnaire(GenerateRequest request, IBaseResource profile, String id) {
        return generateProcessor.generate(request, profile, id);
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

    public PopulateRequest buildPopulateRequest(
            String operationName,
            IBaseResource questionnaire,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        return new PopulateRequest(
                operationName,
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                bundle,
                libraryEngine != null ? libraryEngine : new LibraryEngine(repository, evaluationSettings),
                modelResolver);
    }

    public <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return prePopulate(
                questionnaire,
                patientId,
                parameters,
                bundle,
                useServerData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return prePopulate(
                questionnaire, patientId, parameters, bundle, new LibraryEngine(repository, evaluationSettings));
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
        return prePopulate(
                buildPopulateRequest("prepopulate", questionnaire, subjectId, parameters, bundle, libraryEngine));
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
            Boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return populate(
                questionnaire,
                patientId,
                parameters,
                bundle,
                useServerData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource populate(
            Either3<CanonicalType, IIdType, IBaseResource> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return populate(
                questionnaire, patientId, parameters, bundle, new LibraryEngine(repository, this.evaluationSettings));
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
        return populate(buildPopulateRequest("populate", questionnaire, subjectId, parameters, bundle, libraryEngine));
    }

    public IBaseResource populate(PopulateRequest request) {
        return populateProcessor.populate(request);
    }
}
