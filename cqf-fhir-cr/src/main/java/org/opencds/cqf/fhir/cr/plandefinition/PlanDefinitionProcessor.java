package org.opencds.cqf.fhir.cr.plandefinition;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.PackageHelper.packageParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.common.DataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IDataRequirementsProcessor;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.plandefinition.apply.IApplyProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.Resources;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.npm.NpmPackageLoader;

@SuppressWarnings({"squid:S107", "squid:S1172"})
public class PlanDefinitionProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected IApplyProcessor applyProcessor;
    protected IPackageProcessor packageProcessor;
    protected IDataRequirementsProcessor dataRequirementsProcessor;
    protected org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor activityProcessor;
    protected IRequestResolverFactory requestResolverFactory;
    protected IRepository repository;
    protected NpmPackageLoader npmPackageLoader;
    protected EvaluationSettings evaluationSettings;
    protected TerminologyServerClientSettings terminologyServerClientSettings;

    public PlanDefinitionProcessor(IRepository repository, NpmPackageLoader npmPackageLoader) {
        this(repository, npmPackageLoader, EvaluationSettings.getDefault(), new TerminologyServerClientSettings());
    }

    public PlanDefinitionProcessor(
            IRepository repository,
            NpmPackageLoader npmPackageLoader,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings) {
        this(
                repository,
                npmPackageLoader,
                evaluationSettings,
                terminologyServerClientSettings,
                null,
                null,
                null,
                null,
                null);
    }

    public PlanDefinitionProcessor(
            IRepository repository,
            NpmPackageLoader npmPackageLoader,
            EvaluationSettings evaluationSettings,
            TerminologyServerClientSettings terminologyServerClientSettings,
            IApplyProcessor applyProcessor,
            IPackageProcessor packageProcessor,
            IDataRequirementsProcessor dataRequirementsProcessor,
            org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor activityProcessor,
            IRequestResolverFactory requestResolverFactory) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.npmPackageLoader = requireNonNull(npmPackageLoader, "npmPackageLoader can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        if (packageProcessor == null) {
            this.terminologyServerClientSettings =
                    requireNonNull(terminologyServerClientSettings, "terminologyServerClientSettings can not be null");
        }
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor;
        this.dataRequirementsProcessor = dataRequirementsProcessor;
        this.requestResolverFactory = requestResolverFactory;
        this.activityProcessor = activityProcessor;
        this.applyProcessor = applyProcessor;
    }

    public EvaluationSettings evaluationSettings() {
        return evaluationSettings;
    }

    protected void initApplyProcessor() {
        if (activityProcessor == null) {
            activityProcessor = new org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor(
                    repository,
                    requestResolverFactory != null
                            ? requestResolverFactory
                            : IRequestResolverFactory.getDefault(fhirVersion));
        }
        applyProcessor = applyProcessor != null
                ? applyProcessor
                : new ApplyProcessor(repository, npmPackageLoader, modelResolver, activityProcessor);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolvePlanDefinition(
            Either3<C, IIdType, R> planDefinition) {
        return new ResourceResolver("PlanDefinition", repository).resolve(planDefinition);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packagePlanDefinition(
            Either3<C, IIdType, R> planDefinition) {
        return packagePlanDefinition(planDefinition, false);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packagePlanDefinition(
            Either3<C, IIdType, R> planDefinition, boolean isPut) {
        return packagePlanDefinition(planDefinition, packageParameters(fhirVersion, null, isPut));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packagePlanDefinition(
            Either3<C, IIdType, R> planDefinition, IBaseParameters parameters) {
        return packagePlanDefinition(resolvePlanDefinition(planDefinition), parameters);
    }

    public IBaseBundle packagePlanDefinition(IBaseResource planDefinition, IBaseParameters parameters) {
        var processor = packageProcessor != null
                ? packageProcessor
                : new PackageProcessor(repository, terminologyServerClientSettings);
        return processor.packageResource(planDefinition, parameters);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource dataRequirements(
            Either3<C, IIdType, R> planDefinition, IBaseParameters parameters) {
        return dataRequirements(resolvePlanDefinition(planDefinition), parameters);
    }

    public IBaseResource dataRequirements(IBaseResource planDefinition, IBaseParameters parameters) {
        var processor = dataRequirementsProcessor != null
                ? dataRequirementsProcessor
                : new DataRequirementsProcessor(repository, evaluationSettings);
        return processor.getDataRequirements(planDefinition, parameters);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> ApplyRequest buildApplyRequest(
            @Nonnull Either3<C, IIdType, R> planDefinition,
            @Nonnull String subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException("Missing required parameter: 'subject'");
        }
        return new ApplyRequest(
                resolvePlanDefinition(planDefinition),
                Ids.newId(fhirVersion, Ids.ensureIdType(subject, "Patient")),
                encounter == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(encounter, "Encounter")),
                practitioner == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(practitioner, "Practitioner")),
                organization == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(organization, "Organization")),
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                data,
                prefetchData,
                libraryEngine,
                modelResolver,
                null);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> planDefinition,
            String subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext) {
        return apply(
                planDefinition,
                subject,
                encounter,
                practitioner,
                organization,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                null,
                null,
                null,
                new LibraryEngine(repository, npmPackageLoader, evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> planDefinition,
            String subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return apply(
                planDefinition,
                subject,
                encounter,
                practitioner,
                organization,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                useServerData,
                data,
                prefetchData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> planDefinition,
            String subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return apply(
                planDefinition,
                subject,
                encounter,
                practitioner,
                organization,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                data,
                prefetchData,
                new LibraryEngine(repository, this.npmPackageLoader, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> planDefinition,
            String subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        if (fhirVersion == FhirVersionEnum.R5) {
            return applyR5(
                    planDefinition,
                    List.of(subject),
                    encounter,
                    practitioner,
                    organization,
                    userType,
                    userLanguage,
                    userTaskContext,
                    setting,
                    settingContext,
                    parameters,
                    data,
                    prefetchData,
                    libraryEngine);
        }
        return apply(buildApplyRequest(
                planDefinition,
                subject,
                encounter,
                practitioner,
                organization,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                data,
                prefetchData,
                libraryEngine));
    }

    public IBaseResource apply(ApplyRequest request) {
        initApplyProcessor();
        return applyProcessor.apply(request);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters applyR5(
            Either3<C, IIdType, R> planDefinition,
            List<String> subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return applyR5(
                planDefinition,
                subject,
                encounter,
                practitioner,
                organization,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                useServerData,
                data,
                prefetchData,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters applyR5(
            Either3<C, IIdType, R> planDefinition,
            List<String> subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return applyR5(
                planDefinition,
                subject,
                encounter,
                practitioner,
                organization,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                data,
                prefetchData,
                new LibraryEngine(repository, this.npmPackageLoader, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseParameters applyR5(
            Either3<C, IIdType, R> planDefinition,
            List<String> subject,
            String encounter,
            String practitioner,
            String organization,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine) {
        var param = IAdapterFactory.forFhirVersion(fhirVersion)
                .createParameters((IBaseParameters) Resources.newBaseForVersion("Parameters", fhirVersion));
        subject.forEach(s -> {
            param.addParameter(
                    "return",
                    applyR5(buildApplyRequest(
                            planDefinition,
                            s,
                            encounter,
                            practitioner,
                            organization,
                            userType,
                            userLanguage,
                            userTaskContext,
                            setting,
                            settingContext,
                            parameters,
                            data,
                            prefetchData,
                            libraryEngine)));
        });
        return (IBaseParameters) param.get();
    }

    public IBaseBundle applyR5(ApplyRequest request) {
        initApplyProcessor();
        return applyProcessor.applyR5(request);
    }
}
