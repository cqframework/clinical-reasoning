package org.opencds.cqf.fhir.cr.questionnaire;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.generate.IGenerateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.IPopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateProcessor;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
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

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveQuestionnaire(
            Either3<C, IIdType, R> questionnaire) {
        return (R) questionnaireResolver.resolve(questionnaire);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveStructureDefinition(
            Either3<C, IIdType, R> structureDef) {
        return (R) structureDefResolver.resolve(structureDef);
    }

    public IBaseResource generateQuestionnaire(String id) {
        return generateProcessor.generate(id);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile) {
        return generateQuestionnaire(profile, false, true);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile, Boolean supportedOnly, Boolean requiredOnly) {
        return generateQuestionnaire(
                profile, supportedOnly, requiredOnly, null, null, null, null, (IBaseResource) null, null, null, null);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
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

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
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

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            Boolean supportedOnly,
            Boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            String id) {
        var request = new GenerateRequest(
                resolveStructureDefinition(profile),
                supportedOnly,
                requiredOnly,
                subjectId == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                bundle,
                libraryEngine == null ? new LibraryEngine(repository, evaluationSettings) : libraryEngine,
                modelResolver);
        return generateQuestionnaire(request, id);
    }

    public IBaseResource generateQuestionnaire(GenerateRequest request, String id) {
        return generateProcessor.generate(request, id);
    }

    public <C extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            Either3<C, IIdType, IBaseResource> questionnaire) {
        return packageQuestionnaire(questionnaire, false);
    }

    public <C extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            Either3<C, IIdType, IBaseResource> questionnaire, boolean isPut) {
        return packageQuestionnaire(
                questionnaire,
                newParameters(
                        repository.fhirContext(),
                        "package-parameters",
                        newBooleanPart(repository.fhirContext(), "isPut", isPut)));
    }

    public <C extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            Either3<C, IIdType, IBaseResource> questionnaire, IBaseParameters parameters) {
        return packageQuestionnaire(resolveQuestionnaire(questionnaire), parameters);
    }

    public IBaseBundle packageQuestionnaire(IBaseResource questionnaire, IBaseParameters parameters) {
        return packageProcessor.packageResource(questionnaire, parameters);
    }

    public PopulateRequest buildPopulateRequest(
            String operationName,
            IBaseResource questionnaire,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            LibraryEngine libraryEngine) {
        if (StringUtils.isBlank(subjectId)) {
            throw new IllegalArgumentException("Missing required parameter: 'subject'");
        }
        return new PopulateRequest(
                operationName,
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                bundle,
                useServerData,
                libraryEngine != null ? libraryEngine : new LibraryEngine(repository, evaluationSettings),
                modelResolver);
    }

    // public <C extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
    //         Either3<C, IIdType, R> questionnaire,
    //         String patientId,
    //         IBaseParameters parameters,
    //         IBaseBundle bundle,
    //         Boolean useServerData,
    //         IBaseResource dataEndpoint,
    //         IBaseResource contentEndpoint,
    //         IBaseResource terminologyEndpoint) {
    //     return prePopulate(
    //             questionnaire,
    //             patientId,
    //             parameters,
    //             bundle,
    //             useServerData,
    //             createRestRepository(repository.fhirContext(), dataEndpoint),
    //             createRestRepository(repository.fhirContext(), contentEndpoint),
    //             createRestRepository(repository.fhirContext(), terminologyEndpoint));
    // }

    // public <C extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
    //         Either3<C, IIdType, R> questionnaire,
    //         String patientId,
    //         IBaseParameters parameters,
    //         IBaseBundle bundle,
    //         Boolean useServerData,
    //         Repository dataRepository,
    //         Repository contentRepository,
    //         Repository terminologyRepository) {
    //     repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
    //     return prePopulate(
    //             questionnaire,
    //             patientId,
    //             parameters,
    //             bundle,
    //             useServerData,
    //             new LibraryEngine(repository, evaluationSettings));
    // }

    // public <C extends IPrimitiveType<String>, R extends IBaseResource> R prePopulate(
    //         Either3<C, IIdType, R> questionnaire,
    //         String patientId,
    //         IBaseParameters parameters,
    //         IBaseBundle bundle,
    //         Boolean useServerData,
    //         LibraryEngine libraryEngine) {
    //     return prePopulate(
    //             resolveQuestionnaire(questionnaire), patientId, parameters, bundle, useServerData, libraryEngine);
    // }

    // public <R extends IBaseResource> R prePopulate(
    //         IBaseResource questionnaire,
    //         String subjectId,
    //         IBaseParameters parameters,
    //         IBaseBundle bundle,
    //         Boolean useServerData,
    //         LibraryEngine libraryEngine) {
    //     return prePopulate(buildPopulateRequest(
    //             "prepopulate", questionnaire, subjectId, parameters, bundle, useServerData, libraryEngine));
    // }

    // @SuppressWarnings("unchecked")
    // public <R extends IBaseResource> R prePopulate(PopulateRequest request) {
    //     return (R) populateProcessor.prePopulate(request);
    // }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
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

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return populate(
                questionnaire,
                patientId,
                parameters,
                bundle,
                useServerData,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String patientId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            LibraryEngine libraryEngine) {
        return populate(
                resolveQuestionnaire(questionnaire), patientId, parameters, bundle, useServerData, libraryEngine);
    }

    public IBaseResource populate(
            IBaseResource questionnaire,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            Boolean useServerData,
            LibraryEngine libraryEngine) {
        return populate(buildPopulateRequest(
                "populate", questionnaire, subjectId, parameters, bundle, useServerData, libraryEngine));
    }

    public IBaseResource populate(PopulateRequest request) {
        return populateProcessor.populate(request);
    }
}
