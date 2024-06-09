package org.opencds.cqf.fhir.cr.plandefinition;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.Parameters.newBooleanPart;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.PackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.plandefinition.apply.IApplyProcessor;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public class PlanDefinitionProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected final IApplyProcessor applyProcessor;
    protected final IPackageProcessor packageProcessor;
    protected final org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor activityProcessor;
    protected final IRequestResolverFactory requestResolverFactory;
    protected Repository repository;
    protected EvaluationSettings evaluationSettings;

    public PlanDefinitionProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public PlanDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null, null, null);
    }

    public PlanDefinitionProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IApplyProcessor applyProcessor,
            IPackageProcessor packageProcessor,
            org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor activityProcessor,
            IRequestResolverFactory requestResolverFactory) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        fhirVersion = this.repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.packageProcessor = packageProcessor != null ? packageProcessor : new PackageProcessor(this.repository);
        // These two classes will no longer be needed once we are able to call multiple operations against a
        // HapiFhirRepository
        this.requestResolverFactory = requestResolverFactory != null
                ? requestResolverFactory
                : IRequestResolverFactory.getDefault(fhirVersion);
        this.activityProcessor = activityProcessor != null
                ? activityProcessor
                : new org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor(
                        this.repository, this.requestResolverFactory);
        this.applyProcessor = applyProcessor != null
                ? applyProcessor
                : new ApplyProcessor(this.repository, modelResolver, this.activityProcessor);
    }

    public EvaluationSettings evaluationSettings() {
        return evaluationSettings;
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
        return packagePlanDefinition(
                planDefinition,
                newParameters(
                        repository.fhirContext(),
                        "package-parameters",
                        newBooleanPart(repository.fhirContext(), "isPut", isPut)));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packagePlanDefinition(
            Either3<C, IIdType, R> planDefinition, IBaseParameters parameters) {
        return packagePlanDefinition(resolvePlanDefinition(planDefinition), parameters);
    }

    public IBaseBundle packagePlanDefinition(IBaseResource planDefinition, IBaseParameters parameters) {
        return packageProcessor.packageResource(planDefinition, parameters);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> ApplyRequest buildApplyRequest(
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
            IBaseParameters prefetchData,
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
                useServerData,
                data,
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
                true,
                null,
                null,
                new LibraryEngine(repository, evaluationSettings));
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
            IBaseParameters prefetchData,
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
            IBaseParameters prefetchData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
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
                useServerData,
                data,
                prefetchData,
                new LibraryEngine(repository, this.evaluationSettings));
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
            IBaseParameters prefetchData,
            LibraryEngine libraryEngine) {
        if (fhirVersion == FhirVersionEnum.R5) {
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
                    libraryEngine);
        }
        // TODO: add prefetch bundles to data bundle?
        // this.prefetchData = prefetchData;
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
                useServerData,
                data,
                prefetchData,
                libraryEngine));
    }

    public IBaseResource apply(ApplyRequest request) {
        return applyProcessor.apply(request);
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle applyR5(
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
            IBaseParameters prefetchData,
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

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle applyR5(
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
            IBaseParameters prefetchData,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
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
                useServerData,
                data,
                prefetchData,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle applyR5(
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
            IBaseParameters prefetchData,
            LibraryEngine libraryEngine) {
        // TODO: add prefetch bundles to data bundle?
        // this.prefetchData = prefetchData;
        return applyR5(buildApplyRequest(
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
                libraryEngine));
    }

    public IBaseBundle applyR5(ApplyRequest request) {
        return applyProcessor.applyR5(request);
    }
}
