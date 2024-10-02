package org.opencds.cqf.fhir.cr.questionnaire;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
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
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IPopulateProcessor populateProcessor;

    public QuestionnaireProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public QuestionnaireProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null, null, null);
    }

    public QuestionnaireProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IGenerateProcessor generateProcessor,
            IPackageProcessor packageProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor,
            IPopulateProcessor populateProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.questionnaireResolver = new ResourceResolver("Questionnaire", this.repository);
        this.structureDefResolver = new ResourceResolver("StructureDefinition", this.repository);
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.generateProcessor = generateProcessor;
        this.packageProcessor = packageProcessor;
        this.dataRequirementsProcessor = dataRequirementsProcessor;
        this.populateProcessor = populateProcessor;
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveQuestionnaire(
            Either3<C, IIdType, R> questionnaire) {
        return questionnaireResolver.resolve(questionnaire);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveStructureDefinition(
            Either3<C, IIdType, R> structureDef) {
        return structureDefResolver.resolve(structureDef);
    }

    public IBaseResource generateQuestionnaire(String id) {
        return generateQuestionnaire(null, id);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile) {
        return generateQuestionnaire(profile, false, true);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile, boolean supportedOnly, boolean requiredOnly) {
        return generateQuestionnaire(
                profile, supportedOnly, requiredOnly, null, null, null, true, (IBaseResource) null, null, null, null);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            boolean supportedOnly,
            boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
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
                data,
                useServerData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint),
                id);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            boolean supportedOnly,
            boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
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
                data,
                useServerData,
                new LibraryEngine(repository, evaluationSettings),
                id);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            boolean supportedOnly,
            boolean requiredOnly,
            String subjectId,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            LibraryEngine libraryEngine,
            String id) {
        var request = new GenerateRequest(
                resolveStructureDefinition(profile),
                supportedOnly,
                requiredOnly,
                subjectId == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                parameters,
                useServerData,
                data,
                libraryEngine == null ? new LibraryEngine(repository, evaluationSettings) : libraryEngine,
                modelResolver);
        return generateQuestionnaire(request, id);
    }

    public IBaseResource generateQuestionnaire(GenerateRequest request, String id) {
        var processor = generateProcessor != null ? generateProcessor : new GenerateProcessor(this.repository);
        return request == null ? processor.generate(id) : processor.generate(request, id);
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
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository);
        return processor.packageResource(questionnaire, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> questionnaire, IBaseParameters parameters) {
        return dataRequirements(resolveQuestionnaire(questionnaire), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource questionnaire, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository);
        return processor.getDataRequirements(questionnaire, parameters);
    }

    public PopulateRequest buildPopulateRequest(
            IBaseResource questionnaire,
            String subjectId,
            List<IBase> context,
            IBaseExtension<?, ?> launchContext,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            LibraryEngine libraryEngine) {
        if (StringUtils.isBlank(subjectId)) {
            throw new IllegalArgumentException("Missing required parameter: 'subject'");
        }
        return new PopulateRequest(
                questionnaire,
                Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                context,
                launchContext,
                parameters,
                data,
                useServerData,
                libraryEngine != null ? libraryEngine : new LibraryEngine(repository, evaluationSettings),
                modelResolver);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String subjectId,
            List<IBase> context,
            IBaseExtension<?, ?> launchContext,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return populate(
                questionnaire,
                subjectId,
                context,
                launchContext,
                parameters,
                data,
                useServerData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String subjectId,
            List<IBase> context,
            IBaseExtension<?, ?> launchContext,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return populate(
                questionnaire,
                subjectId,
                context,
                launchContext,
                parameters,
                data,
                useServerData,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String subjectId,
            List<IBase> context,
            IBaseExtension<?, ?> launchContext,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            LibraryEngine libraryEngine) {
        return populate(
                resolveQuestionnaire(questionnaire),
                subjectId,
                context,
                launchContext,
                parameters,
                data,
                useServerData,
                libraryEngine);
    }

    public IBaseResource populate(
            IBaseResource questionnaire,
            String subjectId,
            List<IBase> context,
            IBaseExtension<?, ?> launchContext,
            IBaseParameters parameters,
            IBaseBundle data,
            boolean useServerData,
            LibraryEngine libraryEngine) {
        return populate(buildPopulateRequest(
                questionnaire, subjectId, context, launchContext, parameters, data, useServerData, libraryEngine));
    }

    public IBaseResource populate(PopulateRequest request) {
        var processor = populateProcessor != null ? populateProcessor : new PopulateProcessor();
        return processor.populate(request);
    }
}
