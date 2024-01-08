package org.opencds.cqf.fhir.cr.plandefinition.apply;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.ICompositeType;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IApplyRequest;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;
import org.opencds.cqf.fhir.cr.inputparameters.InputParameterResolverFactory;

public class ApplyRequest implements IApplyRequest {
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
    private final Boolean useServerData;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;
    private final IInputParameterResolver inputParameterResolver;
    private final Collection<IBaseResource> requestResources;
    private final Collection<IBaseResource> extractedResources;
    private IBaseOperationOutcome operationOutcome;
    private IBaseResource questionnaire;
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
            Boolean useServerData,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        this.planDefinition = planDefinition;
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
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        fhirVersion = planDefinition.getStructureFhirVersionEnum();
        defaultLibraryUrl = resolveDefaultLibraryUrl();
        inputParameterResolver = libraryEngine == null
                ? null
                : InputParameterResolverFactory.create(
                        libraryEngine.getRepository(),
                        this.subjectId,
                        this.encounterId,
                        this.practitionerId,
                        this.parameters,
                        this.useServerData,
                        this.bundle);
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
                bundle,
                libraryEngine,
                modelResolver);
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
    public IBaseBundle getBundle() {
        return bundle;
    }

    @Override
    public Boolean getUseServerData() {
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

    public void setQuestionnaire(IBaseResource questionnaire) {
        this.questionnaire = questionnaire;
    }

    @Override
    public IBaseResource getQuestionnaire() {
        return this.questionnaire;
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
}