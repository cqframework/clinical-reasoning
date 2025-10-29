package org.opencds.cqf.fhir.cr.questionnaire;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
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

@SuppressWarnings("UnstableApiUsage")
public class QuestionnaireProcessor {
    protected static final String SUBJECT_TYPE = "Patient";

    protected final ResourceResolver questionnaireResolver;
    protected final ResourceResolver structureDefResolver;
    protected final ModelResolver modelResolver;
    protected final CrSettings crSettings;
    protected final FhirVersionEnum fhirVersion;
    protected IRepository repository;
    protected IGenerateProcessor generateProcessor;
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected IPopulateProcessor populateProcessor;

    public QuestionnaireProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public QuestionnaireProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null);
    }

    public QuestionnaireProcessor(
            IRepository repository, CrSettings crSettings, List<? extends IOperationProcessor> operationProcessors) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.crSettings = requireNonNull(crSettings, "evaluationSettings can not be null");
        this.questionnaireResolver = new ResourceResolver("Questionnaire", this.repository);
        this.structureDefResolver = new ResourceResolver("StructureDefinition", this.repository);
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        if (operationProcessors != null && !operationProcessors.isEmpty()) {
            operationProcessors.forEach(p -> {
                if (p instanceof IPackageProcessor pack) {
                    packageProcessor = pack;
                }
                if (p instanceof IDataRequirementsProcessor dataReq) {
                    dataRequirementsProcessor = dataReq;
                }
                if (p instanceof IGenerateProcessor generate) {
                    generateProcessor = generate;
                }
                if (p instanceof IPopulateProcessor populate) {
                    populateProcessor = populate;
                }
            });
        }
    }

    public CrSettings getSettings() {
        return crSettings;
    }

    public ModelResolver getModelResolver() {
        return modelResolver;
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
        return generateQuestionnaire(profile, supportedOnly, requiredOnly, (IBaseResource) null, null, null);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            boolean supportedOnly,
            boolean requiredOnly,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint,
            String id) {
        return generateQuestionnaire(
                profile,
                supportedOnly,
                requiredOnly,
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint),
                id);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            boolean supportedOnly,
            boolean requiredOnly,
            IRepository contentRepository,
            IRepository terminologyRepository,
            String id) {
        repository = proxy(repository, true, null, contentRepository, terminologyRepository);
        return generateQuestionnaire(profile, supportedOnly, requiredOnly, id);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile, boolean supportedOnly, boolean requiredOnly, String id) {
        return generateQuestionnaire(profile, supportedOnly, requiredOnly, id, null);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource generateQuestionnaire(
            Either3<C, IIdType, R> profile,
            boolean supportedOnly,
            boolean requiredOnly,
            String id,
            LibraryEngine libraryEngine) {
        var request = new GenerateRequest(
                resolveStructureDefinition(profile),
                supportedOnly,
                requiredOnly,
                libraryEngine != null
                        ? libraryEngine
                        : new LibraryEngine(repository, crSettings.getEvaluationSettings()),
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
        return packageQuestionnaire(questionnaire, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>> IBaseBundle packageQuestionnaire(
            Either3<C, IIdType, IBaseResource> questionnaire, IBaseParameters parameters) {
        return packageQuestionnaire(resolveQuestionnaire(questionnaire), parameters);
    }

    public IBaseBundle packageQuestionnaire(IBaseResource questionnaire, IBaseParameters parameters) {
        var processor = packageProcessor != null ? packageProcessor : new PackageProcessor(repository, crSettings);
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
            List<? extends IBaseBackboneElement> context,
            IBaseExtension<?, ?> launchContext,
            IBaseBundle data,
            LibraryEngine libraryEngine) {
        return new PopulateRequest(
                questionnaire,
                subjectId == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, SUBJECT_TYPE)),
                context,
                launchContext,
                data,
                libraryEngine != null
                        ? libraryEngine
                        : new LibraryEngine(repository, crSettings.getEvaluationSettings()),
                modelResolver);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String subjectId,
            List<? extends IBaseBackboneElement> context,
            IBaseExtension<?, ?> launchContext,
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
                data,
                useServerData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String subjectId,
            List<? extends IBaseBackboneElement> context,
            IBaseExtension<?, ?> launchContext,
            IBaseBundle data,
            boolean useServerData,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return populate(
                questionnaire,
                subjectId,
                context,
                launchContext,
                data,
                new LibraryEngine(repository, crSettings.getEvaluationSettings()));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource populate(
            Either3<C, IIdType, R> questionnaire,
            String subjectId,
            List<? extends IBaseBackboneElement> context,
            IBaseExtension<?, ?> launchContext,
            IBaseBundle data,
            LibraryEngine libraryEngine) {
        return populate(resolveQuestionnaire(questionnaire), subjectId, context, launchContext, data, libraryEngine);
    }

    public IBaseResource populate(
            IBaseResource questionnaire,
            String subjectId,
            List<? extends IBaseBackboneElement> context,
            IBaseExtension<?, ?> launchContext,
            IBaseBundle data,
            LibraryEngine libraryEngine) {
        return populate(buildPopulateRequest(questionnaire, subjectId, context, launchContext, data, libraryEngine));
    }

    public IBaseResource populate(PopulateRequest request) {
        var processor = populateProcessor != null ? populateProcessor : new PopulateProcessor();
        return processor.populate(request);
    }
}
