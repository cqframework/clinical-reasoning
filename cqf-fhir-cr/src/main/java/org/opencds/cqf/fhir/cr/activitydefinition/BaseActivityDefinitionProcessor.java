package org.opencds.cqf.fhir.cr.activitydefinition;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBase;
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
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.engine.model.FhirModelResolverCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public abstract class BaseActivityDefinitionProcessor<T> {
    private static final Logger logger = LoggerFactory.getLogger(BaseActivityDefinitionProcessor.class);
    public static final String TARGET_STATUS_URL = "http://hl7.org/fhir/us/ecr/StructureDefinition/targetStatus";
    public static final String PRODUCT_ERROR_PREAMBLE = "Product does not map to ";
    public static final String DOSAGE_ERROR_PREAMBLE = "Dosage does not map to ";
    public static final String BODYSITE_ERROR_PREAMBLE = "BodySite does not map to ";
    public static final String CODE_ERROR_PREAMBLE = "Code does not map to ";
    public static final String QUANTITY_ERROR_PREAMBLE = "Quantity does not map to ";
    public static final String MISSING_CODE_PROPERTY = "Missing required code property";
    protected static final List<String> EXCLUDED_EXTENSION_LIST =
            Arrays.asList(Constants.CPG_KNOWLEDGE_CAPABILITY, Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL);
    protected final ModelResolver modelResolver;
    protected final EvaluationSettings evaluationSettings;
    protected ExtensionResolver extensionResolver;
    protected Repository repository;

    protected String subjectId;
    protected String encounterId;
    protected String practitionerId;
    protected String organizationId;
    protected IBaseParameters parameters;
    protected Boolean useServerData;
    protected IBaseBundle bundle;
    protected LibraryEngine libraryEngine;

    protected BaseActivityDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.repository = requireNonNull(repository, "repository can not be null");
        modelResolver = FhirModelResolverCache.resolverForVersion(
                repository.fhirContext().getVersion().getVersion());
    }

    public static <T extends IBase> Optional<T> castOrThrow(IBase obj, Class<T> type, String errorMessage) {
        if (obj == null) return Optional.empty();
        if (type.isInstance(obj)) {
            return Optional.of(type.cast(obj));
        }
        throw new IllegalArgumentException(errorMessage);
    }

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(
            IIdType id,
            CanonicalType canonical,
            IBaseResource activityDefinition,
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
                id,
                canonical,
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

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(
            IIdType id,
            CanonicalType canonical,
            IBaseResource activityDefinition,
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
                id,
                canonical,
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

    public <CanonicalType extends IPrimitiveType<String>> IBaseResource apply(
            IIdType id,
            CanonicalType canonical,
            IBaseResource activityDefinition,
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
                resolveActivityDefinition(id, canonical, activityDefinition),
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

    public IBaseResource apply(
            T activityDefinition,
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
        this.subjectId = subjectId;
        this.encounterId = encounterId;
        this.practitionerId = practitionerId;
        this.organizationId = organizationId;
        this.parameters = parameters;
        this.useServerData = useServerData;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;

        return applyActivityDefinition(initApply(activityDefinition));
    }

    protected abstract <CanonicalType extends IPrimitiveType<String>> T resolveActivityDefinition(
            IIdType id, CanonicalType canonical, IBaseResource activityDefinition);

    protected abstract T initApply(T activityDefinition);

    protected abstract IBaseResource applyActivityDefinition(T activityDefinition);

    protected void resolveDynamicValue(List<IBase> result, String expression, String path, IBaseResource resource) {
        if (result == null || result.isEmpty()) {
            return;
        }
        if (result.size() > 1) {
            throw new IllegalArgumentException(
                    String.format("Dynamic value resolution received multiple values for expression: %s", expression));
        }
        modelResolver.setValue(resource, path, result.get(0));
    }

    protected FhirContext fhirContext() {
        return repository.fhirContext();
    }
}
