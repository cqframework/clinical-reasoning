package org.opencds.cqf.fhir.cr.plandefinition;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.model.api.IElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.common.IPackageProcessor;
import org.opencds.cqf.fhir.cr.common.ResourceResolver;
import org.opencds.cqf.fhir.cr.plandefinition.apply.ApplyRequest;
import org.opencds.cqf.fhir.cr.plandefinition.apply.IApplyProcessor;
import org.opencds.cqf.fhir.cr.plandefinition.packages.PackageProcessor;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.Ids;
import org.opencds.cqf.fhir.utility.monad.Either3;
import org.opencds.cqf.fhir.utility.repository.Repositories;

@SuppressWarnings({"unused", "squid:S107", "squid:S1172"})
public class PlanDefinitionProcessor {
    // private static final Logger logger = LoggerFactory.getLogger(BasePlanDefinitionProcessor.class);
    protected static final List<String> EXCLUDED_EXTENSION_LIST = Arrays.asList(
            Constants.CPG_KNOWLEDGE_CAPABILITY,
            Constants.CPG_KNOWLEDGE_REPRESENTATION_LEVEL,
            Constants.CQFM_SOFTWARE_SYSTEM,
            Constants.CPG_QUESTIONNAIRE_GENERATE,
            Constants.CQFM_LOGIC_DEFINITION,
            Constants.CQFM_EFFECTIVE_DATA_REQUIREMENTS);

    // protected final OperationParametersParser operationParametersParser;

    protected final ResourceResolver resourceResolver;
    protected final ModelResolver modelResolver;
    protected final IApplyProcessor applyProcessor;
    protected final IPackageProcessor packageProcessor;
    protected Repository repository;
    protected LibraryEngine libraryEngine;
    protected EvaluationSettings evaluationSettings;

    protected ApplyRequest request;

    protected IIdType subjectId;
    protected IIdType encounterId;
    protected IIdType practitionerId;
    protected IIdType organizationId;
    protected IBaseDatatype userType;
    protected IBaseDatatype userLanguage;
    protected IBaseDatatype userTaskContext;
    protected IBaseDatatype setting;
    protected IBaseDatatype settingContext;
    protected IBaseParameters parameters;
    protected Boolean useServerData;
    protected IBaseBundle bundle;
    protected IBaseParameters prefetchData;
    protected Boolean containResources;
    protected IBaseResource questionnaire;
    protected Collection<IBaseResource> requestResources;
    protected Collection<IBaseResource> extractedResources;

    public PlanDefinitionProcessor(Repository repository) {
        this(repository, EvaluationSettings.getDefault());
    }

    public PlanDefinitionProcessor(Repository repository, EvaluationSettings evaluationSettings) {
        this(repository, evaluationSettings, null, null);
    }

    public PlanDefinitionProcessor(
            Repository repository,
            EvaluationSettings evaluationSettings,
            IApplyProcessor applyProcessor,
            IPackageProcessor packageProcessor) {
        this.repository = requireNonNull(repository, "repository can not be null");
        this.evaluationSettings = requireNonNull(evaluationSettings, "evaluationSettings can not be null");
        this.resourceResolver = new ResourceResolver("PlanDefinition", this.repository);
        // this.operationParametersParser = new OperationParametersParser(
        //         Engines.getAdapterFactory(fhirContext()), new FhirTypeConverterFactory().create(fhirVersion()));
        modelResolver = FhirModelResolverCache.resolverForVersion(
                repository.fhirContext().getVersion().getVersion());
        this.applyProcessor = applyProcessor; // == null ? new ApplyProcessor() : applyProcessor;
        this.packageProcessor =
                packageProcessor == null ? new PackageProcessor(this.repository, modelResolver) : packageProcessor;
    }

    public FhirContext fhirContext() {
        return this.repository.fhirContext();
    }

    public FhirVersionEnum fhirVersion() {
        return this.fhirContext().getVersion().getVersion();
    }

    protected <CanonicalType extends IPrimitiveType<String>, R extends IBaseResource> R resolvePlanDefinition(
            Either3<CanonicalType, IIdType, R> planDefinition) {
        return resourceResolver.resolve(planDefinition);
    }

    // public abstract T initApply(T planDefinition);

    // public abstract IBaseResource applyPlanDefinition(T planDefinition);

    // public abstract IBaseResource transformToCarePlan(IBaseResource requestGroup);

    // public abstract IBaseResource transformToBundle(IBaseResource requestGroup);

    // protected abstract void extractQuestionnaireResponse();

    // protected abstract void addOperationOutcomeIssue(String issue);

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseBundle packagePlanDefinition(
            Either3<C, IIdType, R> planDefinition, boolean isPut) {
        return packageProcessor.packageResource(resolvePlanDefinition(planDefinition), isPut ? "PUT" : "POST");
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
            Boolean useServerData,
            IBaseBundle bundle,
            IBaseParameters prefetchData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
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
                bundle,
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
            Boolean useServerData,
            IBaseBundle bundle,
            IBaseParameters prefetchData,
            LibraryEngine libraryEngine) {
        if (fhirVersion() == FhirVersionEnum.R5) {
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
                    bundle,
                    prefetchData,
                    libraryEngine);
        }
        this.subjectId = Ids.newId(fhirVersion(), Ids.ensureIdType(subject, "Patient"));
        this.encounterId =
                encounter == null ? null : Ids.newId(fhirVersion(), Ids.ensureIdType(encounter, "Encounter"));
        this.practitionerId =
                practitioner == null ? null : Ids.newId(fhirVersion(), Ids.ensureIdType(practitioner, "Practitioner"));
        this.organizationId =
                organization == null ? null : Ids.newId(fhirVersion(), Ids.ensureIdType(organization, "Organization"));
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
        this.parameters = parameters;
        this.useServerData = useServerData;
        this.bundle = bundle;
        this.prefetchData = prefetchData;
        this.libraryEngine = libraryEngine;
        this.containResources = true;
        this.requestResources = new ArrayList<>();
        this.extractedResources = new ArrayList<>();
        // return transformToCarePlan(
        //         applyPlanDefinition(initApply(resolvePlanDefinition(planDefinition))));
        return applyProcessor.apply();
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource applyR5(
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
            Boolean useServerData,
            IBaseBundle bundle,
            IBaseParameters prefetchData,
            IBaseResource dataEndpoint,
            IBaseResource contentEndpoint,
            IBaseResource terminologyEndpoint) {
        repository = Repositories.proxy(repository, dataEndpoint, contentEndpoint, terminologyEndpoint);
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
                bundle,
                prefetchData,
                new LibraryEngine(repository, this.evaluationSettings));
    }

    public <C extends IPrimitiveType<String>, R extends IBaseResource> IBaseResource applyR5(
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
            Boolean useServerData,
            IBaseBundle bundle,
            IBaseParameters prefetchData,
            LibraryEngine libraryEngine) {
        this.subjectId = Ids.newId(fhirVersion(), Ids.ensureIdType(subject, "Patient"));
        this.encounterId =
                encounter == null ? null : Ids.newId(fhirVersion(), Ids.ensureIdType(encounter, "Encounter"));
        this.practitionerId =
                practitioner == null ? null : Ids.newId(fhirVersion(), Ids.ensureIdType(practitioner, "Practitioner"));
        this.organizationId =
                organization == null ? null : Ids.newId(fhirVersion(), Ids.ensureIdType(organization, "Organization"));
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
        this.parameters = parameters;
        this.useServerData = useServerData;
        this.bundle = bundle;
        this.prefetchData = prefetchData;
        this.libraryEngine = libraryEngine;
        this.containResources = false;
        this.requestResources = new ArrayList<>();
        this.extractedResources = new ArrayList<>();
        // return transformToBundle(applyPlanDefinition(initApply(resolvePlanDefinition(planDefinition))));
        return applyProcessor.apply();
    }

    protected void resolveDynamicValue(List<IBase> result, String path, IElement requestAction, IBase resource) {
        if (result == null || result.isEmpty()) {
            return;
        }

        // Strip % so it is supported as defined in the spec
        path = path.replace("%", "");
        var value = result.size() == 1 ? result.get(0) : result;
        if (path.startsWith("activity.extension") || path.startsWith("action.extension")) {
            // Custom logic to handle setting the indicator of a CDS Card because RequestGroup.action does not have a
            // priority property in DSTU3
            if (repository.fhirContext().getVersion().getVersion() != FhirVersionEnum.DSTU3) {
                throw new IllegalArgumentException(
                        "Please use the priority path when setting indicator values when using FHIR R4 or higher for CDS Hooks evaluation");
            }
            // default to adding extension to last action
            ((org.hl7.fhir.dstu3.model.Element) requestAction).addExtension().setValue((org.hl7.fhir.dstu3.model.Type)
                    value);
        } else if (path.startsWith("action.") || resource == null) {
            modelResolver.setValue(requestAction, path.replace("action.", ""), value);
        } else {
            modelResolver.setValue(resource, path, value);
        }
    }
}
