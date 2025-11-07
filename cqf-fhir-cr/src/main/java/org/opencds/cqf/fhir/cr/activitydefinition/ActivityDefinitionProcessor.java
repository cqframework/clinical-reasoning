package org.opencds.cqf.fhir.cr.activitydefinition;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.CrSettings;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.common.IOperationProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessor;

@SuppressWarnings("UnstableApiUsage")
public class ActivityDefinitionProcessor implements IActivityDefinitionProcessor {
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected final ResourceResolver resourceResolver;
    protected IApplyProcessor applyProcessor;
    protected IRequestResolverFactory requestResolverFactory;
    protected IRepository repository;
    protected CrSettings crSettings;
    protected ExtensionResolver extensionResolver;

    public ActivityDefinitionProcessor(IRepository repository) {
        this(repository, CrSettings.getDefault());
    }

    public ActivityDefinitionProcessor(IRepository repository, CrSettings crSettings) {
        this(repository, crSettings, null, null);
    }

    public ActivityDefinitionProcessor(
            IRepository repository,
            CrSettings crSettings,
            IRequestResolverFactory requestResolverFactory,
            List<? extends IOperationProcessor> operationProcessors) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.crSettings = requireNonNull(crSettings, "crSettings can not be null");
        this.resourceResolver = new ResourceResolver("ActivityDefinition", this.repository);
        fhirVersion = repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.requestResolverFactory = requestResolverFactory;
        if (operationProcessors != null && !operationProcessors.isEmpty()) {
            operationProcessors.forEach(p -> {
                if (p instanceof IApplyProcessor apply) {
                    applyProcessor = apply;
                }
            });
        }
    }

    @Override
    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> activityDefinition,
            String subjectId,
            String encounterId,
            String practitionerId,
            String organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext) {
        return apply(
                activityDefinition,
                subjectId,
                encounterId,
                practitionerId,
                organizationId,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                null,
                true,
                null,
                (IBaseResource) null,
                null,
                null);
    }

    @Override
    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> activityDefinition,
            String subjectId,
            String encounterId,
            String practitionerId,
            String organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        return apply(
                activityDefinition,
                subjectId,
                encounterId,
                practitionerId,
                organizationId,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                useServerData,
                data,
                createRestRepository(repository.fhirContext(), dataEndpoint),
                createRestRepository(repository.fhirContext(), contentEndpoint),
                createRestRepository(repository.fhirContext(), terminologyEndpoint));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> activityDefinition,
            String subjectId,
            String encounterId,
            String practitionerId,
            String organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            IRepository dataRepository,
            IRepository contentRepository,
            IRepository terminologyRepository) {
        repository = proxy(repository, useServerData, dataRepository, contentRepository, terminologyRepository);
        return apply(
                activityDefinition,
                subjectId,
                encounterId,
                practitionerId,
                organizationId,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                data,
                new LibraryEngine(repository, crSettings.getEvaluationSettings()));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource apply(
            Either3<C, IIdType, R> activityDefinition,
            String subjectId,
            String encounterId,
            String practitionerId,
            String organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            IBaseBundle data,
            LibraryEngine libraryEngine) {
        if (StringUtils.isBlank(subjectId)) {
            throw new IllegalArgumentException("Missing required parameter: 'subject'");
        }
        final ApplyRequest request = buildApplyRequest(
                activityDefinition,
                subjectId,
                encounterId,
                practitionerId,
                organizationId,
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                data,
                libraryEngine);
        initApplyProcessor();
        return applyProcessor.apply(request);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> ApplyRequest buildApplyRequest(
            Either3<C, IIdType, R> activityDefinition,
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
            LibraryEngine libraryEngine) {
        if (StringUtils.isBlank(subject)) {
            throw new IllegalArgumentException("Missing required parameter: 'subject'");
        }
        return new ApplyRequest(
                resolveActivityDefinition(activityDefinition),
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
                libraryEngine,
                modelResolver);
    }

    protected void initApplyProcessor() {
        if (applyProcessor == null) {
            applyProcessor = new ApplyProcessor(
                    repository,
                    requestResolverFactory != null
                            ? requestResolverFactory
                            : IRequestResolverFactory.getDefault(fhirVersion));
        }
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveActivityDefinition(
            Either3<C, IIdType, R> activityDefinition) {
        return resourceResolver.resolve(activityDefinition);
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }
}
