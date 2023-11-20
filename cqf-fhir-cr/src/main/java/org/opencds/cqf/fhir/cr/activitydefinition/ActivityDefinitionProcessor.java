package org.opencds.cqf.fhir.cr.activitydefinition;

import static java.util.Objects.requireNonNull;

import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.EvaluationSettings;
import org.opencds.cqf.fhir.cql.ExtensionResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.ResourceResolver;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyProcessor;
import org.opencds.cqf.fhir.cr.activitydefinition.apply.IRequestResolverFactory;
import org.opencds.cqf.fhir.utility.monad.Either3;

import ca.uhn.fhir.context.FhirContext;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public class ActivityDefinitionProcessor {
    // private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProcessor.class);
    protected final EvaluationSettings evaluationSettings;
    protected final IRequestResolverFactory requestResolverFactory;
    protected Repository repository;
    protected ExtensionResolver extensionResolver;
    protected ResourceResolver resourceResolver;

    public ActivityDefinitionProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault(), null);
    }

    public ActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null);
    }

    public ActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings, IRequestResolverFactory requestResolverFactory) {
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.repository = requireNonNull(repository, "repository can not be null");
        this.resourceResolver = new ResourceResolver("ActivityDefinition", this.repository);
        this.requestResolverFactory = requestResolverFactory == null ? getDefaultRequestResolverFactory() : requestResolverFactory;
    }

    private IRequestResolverFactory getDefaultRequestResolverFactory() {
        var fhirVersion = repository.fhirContext().getVersion().getVersion();
        switch (fhirVersion) {
            case DSTU3:
                return new org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.Dstu3ResolverFactory();
            case R4:
                return new org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.R4ResolverFactory();
            case R5:
                return new org.opencds.cqf.fhir.cr.activitydefinition.apply.resolvers.R5ResolverFactory();
            default:
                throw new IllegalArgumentException(String.format("No default resolver factory exists for FHIR version: %s", fhirVersion));
        }
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
                new LibraryEngine(this.repository, this.evaluationSettings));
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
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        this.repository = org.opencds.cqf.fhir.utility.repository.Repositories.proxy(
                repository, dataEndpoint, contentEndpoint, terminologyEndpoint);

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
                new LibraryEngine(this.repository, this.evaluationSettings));
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
        return apply(
                resolveActivityDefinition(activityDefinition),
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
                libraryEngine);
    }

    public <R extends IBaseResource> IBaseResource apply(
            R activityDefinition,
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
        return new ApplyProcessor(this.repository, activityDefinition, libraryEngine, requestResolverFactory)
            .applyActivityDefinition(subjectId, encounterId, practitionerId, organizationId, userType, userLanguage, userTaskContext, setting, settingContext, parameters, useServerData, bundle);
    }

    protected <C extends IPrimitiveType<String>, R extends IBaseResource> R resolveActivityDefinition(
            Either3<C, IIdType, R> activityDefinition) {
        return resourceResolver.resolve(activityDefinition);
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }
}
