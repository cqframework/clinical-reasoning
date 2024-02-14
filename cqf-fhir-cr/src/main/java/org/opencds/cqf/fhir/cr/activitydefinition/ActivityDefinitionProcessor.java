package org.opencds.cqf.fhir.cr.activitydefinition;

import static java.util.Objects.requireNonNull;
import static org.opencds.cqf.fhir.utility.repository.Repositories.createRestRepository;
import static org.opencds.cqf.fhir.utility.repository.Repositories.proxy;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IApplyProcessor;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.operations.IActivityDefinitionProcessor;

public class ActivityDefinitionProcessor implements IActivityDefinitionProcessor {
    // private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProcessor.class);
    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected final FhirVersionEnum fhirVersion;
    protected final ResourceResolver resourceResolver;
    protected final IApplyProcessor applyProcessor;
    protected final IRequestResolverFactory requestResolverFactory;
    protected Repository repository;
    protected ExtensionResolver extensionResolver;

    public ActivityDefinitionProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public ActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null);
    }

    public ActivityDefinitionProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IApplyProcessor applyProcessor,
            IRequestResolverFactory requestResolverFactory) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.resourceResolver = new ResourceResolver("ActivityDefinition", this.repository);
        fhirVersion = repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.requestResolverFactory = requestResolverFactory == null
                ? IRequestResolverFactory.getDefault(fhirVersion)
                : requestResolverFactory;
        this.applyProcessor = applyProcessor != null
                ? applyProcessor
                : new ApplyProcessor(this.repository, this.requestResolverFactory);
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
            Boolean useServerData,
            IBaseBundle bundle,
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
                bundle,
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
            Boolean useServerData,
            IBaseBundle bundle,
            Repository dataRepository,
            Repository contentRepository,
            Repository terminologyRepository) {
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
                useServerData,
                bundle,
                new LibraryEngine(repository, evaluationSettings));
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
            Boolean useServerData,
            IBaseBundle bundle,
            LibraryEngine libraryEngine) {
        final ApplyRequest request = new ApplyRequest(
                resolveActivityDefinition(activityDefinition),
                Ids.newId(fhirVersion, Ids.ensureIdType(subjectId, "Patient")),
                encounterId == null ? null : Ids.newId(fhirVersion, Ids.ensureIdType(encounterId, "Encounter")),
                practitionerId == null
                        ? null
                        : Ids.newId(fhirVersion, Ids.ensureIdType(practitionerId, "Practitioner")),
                organizationId == null
                        ? null
                        : Ids.newId(fhirVersion, Ids.ensureIdType(organizationId, "Organization")),
                userType,
                userLanguage,
                userTaskContext,
                setting,
                settingContext,
                parameters,
                useServerData,
                bundle,
                libraryEngine,
                modelResolver);
        return applyProcessor.apply(request);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveActivityDefinition(
            Either3<C, IIdType, R> activityDefinition) {
        return resourceResolver.resolve(activityDefinition);
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }
}
