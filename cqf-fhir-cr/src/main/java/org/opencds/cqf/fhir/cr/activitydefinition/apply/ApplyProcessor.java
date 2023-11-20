package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.api.Repository;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.inputparameters.InputParameterResolverFactory;
import org.opencds.cqf.fhir.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ApplyProcessor.class);
    protected static final List<String> EXCLUDED_EXTENSION_LIST =
            Arrays.asList(Constants.CPG_KNOWLEDGE_CAPABILITY, Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL);

    protected final Repository repository;
    protected final IBaseResource activityDefinition;
    protected final ModelResolver modelResolver;
    protected final FhirVersionEnum fhirVersion;
    protected final LibraryEngine libraryEngine;
    protected final IRequestResolverFactory resolverFactory;
    protected final ExtensionProcessor extensionProcessor;
    protected final DynamicValueProcessor dynamicValueProcessor;

    protected String subjectId;
    protected String encounterId;
    protected String practitionerId;
    protected String organizationId;
    protected IBaseParameters parameters;
    protected Boolean useServerData;
    protected IBaseBundle bundle;
    protected String defaultLibraryUrl;
    protected IInputParameterResolver inputParameterResolver;

    public ApplyProcessor(
            Repository repository,
            IBaseResource activityDefinition,
            LibraryEngine libraryEngine,
            IRequestResolverFactory resolverFactory) {
        this.repository = repository;
        this.activityDefinition = activityDefinition;
        this.libraryEngine = libraryEngine;
        this.resolverFactory = resolverFactory;
        fhirVersion = repository.fhirContext().getVersion().getVersion();
        modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.extensionProcessor = new ExtensionProcessor(libraryEngine, modelResolver);
        this.dynamicValueProcessor = new DynamicValueProcessor(libraryEngine, modelResolver, fhirVersion);
    }

    public IBaseResource applyActivityDefinition(
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
            IBaseBundle bundle) {
        logger.info(
                "Performing $apply operation on {}",
                activityDefinition.getIdElement().getIdPart());

        this.subjectId = subjectId;
        this.encounterId = encounterId;
        this.practitionerId = practitionerId;
        this.organizationId = organizationId;
        this.parameters = parameters;
        this.useServerData = useServerData;
        this.bundle = bundle;
        defaultLibraryUrl = getDefaultLibraryUrl();
        inputParameterResolver = InputParameterResolverFactory.create(
                repository, subjectId, encounterId, practitionerId, parameters, useServerData, bundle);

        var resourceResolver = resolverFactory.create(activityDefinition);
        var result = resourceResolver.resolve(subjectId, encounterId, practitionerId, organizationId);

        resolveMeta(result);
        resolveExtensions(result);
        resolveDynamicValues(result);

        return result;
    }

    protected String getDefaultLibraryUrl() {
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.ActivityDefinition) activityDefinition).hasLibrary()
                        ? ((org.hl7.fhir.dstu3.model.ActivityDefinition) activityDefinition)
                                .getLibrary()
                                .get(0)
                                .getReference()
                        : null;
            case R4:
                return ((org.hl7.fhir.r4.model.ActivityDefinition) activityDefinition).hasLibrary()
                        ? ((org.hl7.fhir.r4.model.ActivityDefinition) activityDefinition)
                                .getLibrary()
                                .get(0)
                                .getValueAsString()
                        : null;
            case R5:
                return ((org.hl7.fhir.r5.model.ActivityDefinition) activityDefinition).hasLibrary()
                        ? ((org.hl7.fhir.r5.model.ActivityDefinition) activityDefinition)
                                .getLibrary()
                                .get(0)
                                .getValueAsString()
                        : null;
            default:
                return null;
        }
    }

    protected void resolveMeta(IBaseResource resource) {
        switch (fhirVersion) {
            case DSTU3:
                // Dstu3 does not have a profile property on ActivityDefinition so we are not resolving meta
                break;
            case R4:
                if (((org.hl7.fhir.r4.model.ActivityDefinition) activityDefinition).hasProfile()) {
                    var meta = new org.hl7.fhir.r4.model.Meta();
                    meta.addProfile(((org.hl7.fhir.r4.model.ActivityDefinition) activityDefinition).getProfile());
                    ((org.hl7.fhir.r4.model.DomainResource) resource).setMeta(meta);
                }
                break;
            case R5:
                if (((org.hl7.fhir.r5.model.ActivityDefinition) activityDefinition).hasProfile()) {
                    var meta = new org.hl7.fhir.r5.model.Meta();
                    meta.addProfile(((org.hl7.fhir.r5.model.ActivityDefinition) activityDefinition).getProfile());
                    ((org.hl7.fhir.r5.model.DomainResource) resource).setMeta(meta);
                }
                break;
            default:
                break;
        }
    }

    protected void resolveExtensions(IBaseResource resource) {
        extensionProcessor.processExtensions(
                subjectId,
                bundle,
                resource,
                activityDefinition,
                defaultLibraryUrl,
                inputParameterResolver.getParameters());
    }

    protected void resolveDynamicValues(IBaseResource resource) {
        dynamicValueProcessor.processDynamicValues(
                subjectId,
                bundle,
                resource,
                activityDefinition,
                defaultLibraryUrl,
                inputParameterResolver.getParameters());
    }
}
