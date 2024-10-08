package org.opencds.cqf.fhir.cr.plandefinition.apply;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver.createResolver;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ACTIVITY_DEFINITION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_DATA;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ENCOUNTER;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_ORGANIZATION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PARAMETERS;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PLAN_DEFINITION;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_PRACTITIONER;
import static org.opencds.cqf.fhir.utility.Constants.APPLY_PARAMETER_SUBJECT;
import static org.opencds.cqf.fhir.utility.Parameters.newPart;
import static org.opencds.cqf.fhir.utility.Parameters.newStringPart;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseExtension;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.questionnaire.generate.GenerateRequest;
import org.opencds.cqf.fhir.cr.questionnaire.populate.PopulateRequest;
import org.opencds.cqf.fhir.utility.Constants;
import org.opencds.cqf.fhir.utility.adapter.QuestionnaireAdapter;

public class ApplyRequest implements ICpgRequest {
    private static final String ACTIVITY_DEFINITION = "ActivityDefinition";
    private final IBaseResource planDefinition;
    private final IIdType subjectId;
    private final IIdType encounterId;
    private final IIdType practitionerId;
    private final IIdType organizationId;
    private final IBaseDatatype userType;
    private final IBaseDatatype userLanguage;
    private final IBaseDatatype userTaskContext;
    private final IBaseDatatype setting;
    private final IBaseDatatype settingContext;
    private final IBaseParameters parameters;
    private final boolean useServerData;
    private IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;
    private final IInputParameterResolver inputParameterResolver;
    private final Collection<IBaseResource> requestResources;
    private final Collection<IBaseResource> extractedResources;
    private IBaseOperationOutcome operationOutcome;
    private IBaseResource questionnaire;
    private QuestionnaireAdapter questionnaireAdapter;
    private Boolean containResources;

    public ApplyRequest(
            IBaseResource planDefinition,
            IIdType subjectId,
            IIdType encounterId,
            IIdType practitionerId,
            IIdType organizationId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            IInputParameterResolver inputParameterResolver) {
        checkNotNull(planDefinition, "expected non-null value for planDefinition");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.planDefinition = planDefinition;
        fhirVersion = planDefinition.getStructureFhirVersionEnum();
        this.subjectId = subjectId;
        this.encounterId = encounterId;
        this.practitionerId = practitionerId;
        this.organizationId = organizationId;
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
        this.parameters = parameters;
        this.useServerData = useServerData;
        if (prefetchData != null && !prefetchData.isEmpty()) {
            if (data == null) {
                data = newBundle(fhirVersion);
            }
            resolvePrefetchData(data, prefetchData);
        }
        this.data = data;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.inputParameterResolver = inputParameterResolver != null
                ? inputParameterResolver
                : createResolver(
                        libraryEngine.getRepository(),
                        this.subjectId,
                        this.encounterId,
                        this.practitionerId,
                        this.parameters,
                        this.useServerData,
                        this.data);
        defaultLibraryUrl = resolveDefaultLibraryUrl();
        requestResources = new ArrayList<>();
        extractedResources = new ArrayList<>();
        containResources = false;
    }

    public ApplyRequest copy(IBaseResource planDefinition) {
        return new ApplyRequest(
                        planDefinition,
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
                        null,
                        libraryEngine,
                        modelResolver,
                        inputParameterResolver)
                .setQuestionnaire(questionnaire);
    }

    public org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyRequest toActivityRequest(
            IBaseResource activityDefinition) {
        return new org.opencds.cqf.fhir.cr.activitydefinition.apply.ApplyRequest(
                activityDefinition,
                getSubjectId(),
                getEncounterId(),
                getPractitionerId(),
                getOrganizationId(),
                getUserType(),
                getUserLanguage(),
                getUserTaskContext(),
                getSetting(),
                getSettingContext(),
                getParameters(),
                getUseServerData(),
                getData(),
                libraryEngine,
                modelResolver);
    }

    public GenerateRequest toGenerateRequest(IBaseResource profile) {
        return new GenerateRequest(profile, false, true, modelResolver)
                .setDefaultLibraryUrl(defaultLibraryUrl)
                .setQuestionnaire(questionnaire);
    }

    public PopulateRequest toPopulateRequest() {
        List<IBaseBackboneElement> context = new ArrayList<>();
        var launchContextExts =
                getQuestionnaireAdapter().getExtensionsByUrl(Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT);
        launchContextExts.forEach(lc -> {
            var code = lc.getExtension().stream()
                    .map(c -> (IBaseExtension<?, ?>) c)
                    .filter(c -> c.getUrl().equals("name"))
                    .map(c -> resolvePathString(c.getValue(), "code"))
                    .findFirst()
                    .orElse(null);
            String value = null;
            switch (Constants.SDC_QUESTIONNAIRE_LAUNCH_CONTEXT_CODE.valueOf(code.toUpperCase())) {
                case PATIENT:
                    value = subjectId.getValue();
                    break;
                case ENCOUNTER:
                    value = encounterId.getValue();
                    break;
                case USER:
                    value = practitionerId.getValue();
                    break;
                default:
                    break;
            }
            context.add((IBaseBackboneElement) newPart(
                    getFhirContext(),
                    "context",
                    newStringPart(getFhirContext(), "name", code),
                    newPart(getFhirContext(), "Reference", "content", value)));
        });
        return new PopulateRequest(
                questionnaire, subjectId, context, null, parameters, data, useServerData, libraryEngine, modelResolver);
    }

    public IBaseResource getPlanDefinition() {
        return planDefinition;
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    @Override
    public IIdType getPractitionerId() {
        return practitionerId;
    }

    @Override
    public IIdType getEncounterId() {
        return encounterId;
    }

    @Override
    public IIdType getOrganizationId() {
        return organizationId;
    }

    @Override
    public IBaseDatatype getUserType() {
        return userType;
    }

    @Override
    public IBaseDatatype getUserLanguage() {
        return userLanguage;
    }

    @Override
    public IBaseDatatype getUserTaskContext() {
        return userTaskContext;
    }

    @Override
    public IBaseDatatype getSetting() {
        return setting;
    }

    @Override
    public IBaseDatatype getSettingContext() {
        return settingContext;
    }

    @Override
    public IBaseBundle getData() {
        return data;
    }

    @Override
    public boolean getUseServerData() {
        return useServerData;
    }

    @Override
    public IBaseParameters getParameters() {
        return inputParameterResolver.getParameters();
    }

    @Override
    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    @Override
    public ModelResolver getModelResolver() {
        return modelResolver;
    }

    @Override
    public FhirVersionEnum getFhirVersion() {
        return fhirVersion;
    }

    @Override
    public String getDefaultLibraryUrl() {
        return defaultLibraryUrl;
    }

    @Override
    public IBaseOperationOutcome getOperationOutcome() {
        return operationOutcome;
    }

    @Override
    public void setOperationOutcome(IBaseOperationOutcome operationOutcome) {
        this.operationOutcome = operationOutcome;
    }

    @Override
    public String getOperationName() {
        return "apply";
    }

    @Override
    public IBaseResource getQuestionnaire() {
        return questionnaire;
    }

    @Override
    public QuestionnaireAdapter getQuestionnaireAdapter() {
        if (questionnaireAdapter == null && questionnaire != null) {
            questionnaireAdapter = (QuestionnaireAdapter)
                    getAdapterFactory().createKnowledgeArtifactAdapter((IDomainResource) questionnaire);
        }
        return questionnaireAdapter;
    }

    public ApplyRequest setQuestionnaire(IBaseResource questionnaire) {
        this.questionnaire = questionnaire;
        return this;
    }

    public ApplyRequest setData(IBaseBundle bundle) {
        data = bundle;
        return this;
    }

    public void setContainResources(Boolean value) {
        containResources = value;
    }

    public Boolean getContainResources() {
        return containResources;
    }

    public Collection<IBaseResource> getRequestResources() {
        return requestResources;
    }

    public Collection<IBaseResource> getExtractedResources() {
        return extractedResources;
    }

    public <T extends ICompositeType> IBaseParameters resolveInputParameters(List<T> input) {
        return inputParameterResolver.resolveInputParameters(input);
    }

    public IBaseParameters transformRequestParameters(IBaseResource resource) {
        switch (fhirVersion) {
            case DSTU3:
                return transformRequestParametersDstu3(resource);
            case R4:
                return transformRequestParametersR4(resource);
            case R5:
                return transformRequestParametersR5(resource);

            default:
                return null;
        }
    }

    protected IBaseParameters transformRequestParametersDstu3(IBaseResource resource) {
        var resourceParameter = resource.fhirType().equals(ACTIVITY_DEFINITION)
                ? APPLY_PARAMETER_ACTIVITY_DEFINITION
                : APPLY_PARAMETER_PLAN_DEFINITION;
        var params = org.opencds.cqf.fhir.utility.dstu3.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                        resourceParameter, (org.hl7.fhir.dstu3.model.Resource) resource))
                .addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                        APPLY_PARAMETER_SUBJECT, getSubjectId().getValue()));
        if (hasEncounterId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                    APPLY_PARAMETER_ENCOUNTER, getEncounterId().getValue()));
        }
        if (hasPractitionerId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                    APPLY_PARAMETER_PRACTITIONER, getPractitionerId().getValue()));
        }
        if (hasOrganizationId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                    APPLY_PARAMETER_ORGANIZATION, getOrganizationId().getValue()));
        }
        if (getParameters() != null) {
            params.addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                    APPLY_PARAMETER_PARAMETERS, (org.hl7.fhir.dstu3.model.Parameters) getParameters()));
        }
        if (getData() != null) {
            params.addParameter(org.opencds.cqf.fhir.utility.dstu3.Parameters.part(
                    APPLY_PARAMETER_DATA, (org.hl7.fhir.dstu3.model.Resource) getData()));
        }

        return params;
    }

    protected IBaseParameters transformRequestParametersR4(IBaseResource resource) {
        var resourceParameter = resource.fhirType().equals(ACTIVITY_DEFINITION)
                ? APPLY_PARAMETER_ACTIVITY_DEFINITION
                : APPLY_PARAMETER_PLAN_DEFINITION;
        var params = org.opencds.cqf.fhir.utility.r4.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                        resourceParameter, (org.hl7.fhir.r4.model.Resource) resource))
                .addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                        APPLY_PARAMETER_SUBJECT, getSubjectId().getValue()));
        if (hasEncounterId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                    APPLY_PARAMETER_ENCOUNTER, getEncounterId().getValue()));
        }
        if (hasPractitionerId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                    APPLY_PARAMETER_PRACTITIONER, getPractitionerId().getValue()));
        }
        if (hasOrganizationId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                    APPLY_PARAMETER_ORGANIZATION, getOrganizationId().getValue()));
        }
        if (getParameters() != null) {
            params.addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                    APPLY_PARAMETER_PARAMETERS, (org.hl7.fhir.r4.model.Parameters) getParameters()));
        }
        if (getData() != null) {
            params.addParameter(org.opencds.cqf.fhir.utility.r4.Parameters.part(
                    APPLY_PARAMETER_DATA, (org.hl7.fhir.r4.model.Resource) getData()));
        }

        return params;
    }

    protected IBaseParameters transformRequestParametersR5(IBaseResource resource) {
        var resourceParameter = resource.fhirType().equals(ACTIVITY_DEFINITION)
                ? APPLY_PARAMETER_ACTIVITY_DEFINITION
                : APPLY_PARAMETER_PLAN_DEFINITION;
        var params = org.opencds.cqf.fhir.utility.r5.Parameters.parameters()
                .addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                        resourceParameter, (org.hl7.fhir.r5.model.Resource) resource))
                .addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                        APPLY_PARAMETER_SUBJECT, getSubjectId().getValue()));
        if (hasEncounterId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                    APPLY_PARAMETER_ENCOUNTER, getEncounterId().getValue()));
        }
        if (hasPractitionerId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                    APPLY_PARAMETER_PRACTITIONER, getPractitionerId().getValue()));
        }
        if (hasOrganizationId()) {
            params.addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                    APPLY_PARAMETER_ORGANIZATION, getOrganizationId().getValue()));
        }
        if (getParameters() != null) {
            params.addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                    APPLY_PARAMETER_PARAMETERS, (org.hl7.fhir.r5.model.Parameters) getParameters()));
        }
        if (getData() != null) {
            params.addParameter(org.opencds.cqf.fhir.utility.r5.Parameters.part(
                    APPLY_PARAMETER_DATA, (org.hl7.fhir.r5.model.Resource) getData()));
        }

        return params;
    }

    protected final String resolveDefaultLibraryUrl() {
        switch (fhirVersion) {
            case DSTU3:
                return ((org.hl7.fhir.dstu3.model.PlanDefinition) planDefinition).hasLibrary()
                        ? ((org.hl7.fhir.dstu3.model.PlanDefinition) planDefinition)
                                .getLibrary()
                                .get(0)
                                .getReference()
                        : null;
            case R4:
                return ((org.hl7.fhir.r4.model.PlanDefinition) planDefinition).hasLibrary()
                        ? ((org.hl7.fhir.r4.model.PlanDefinition) planDefinition)
                                .getLibrary()
                                .get(0)
                                .getValueAsString()
                        : null;
            case R5:
                return ((org.hl7.fhir.r5.model.PlanDefinition) planDefinition).hasLibrary()
                        ? ((org.hl7.fhir.r5.model.PlanDefinition) planDefinition)
                                .getLibrary()
                                .get(0)
                                .getValueAsString()
                        : null;
            default:
                return null;
        }
    }
}
