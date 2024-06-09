package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
import org.opencds.cqf.fhir.cr.inputparameters.IInputParameterResolver;

public class ApplyRequest implements ICpgRequest {
    private final IBaseResource activityDefinition;
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
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;
    private final IInputParameterResolver inputParameterResolver;
    private IBaseOperationOutcome operationOutcome;

    public ApplyRequest(
            IBaseResource activityDefinition,
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
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.activityDefinition = activityDefinition;
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
        this.data = data;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        fhirVersion = activityDefinition.getStructureFhirVersionEnum();
        defaultLibraryUrl = resolveDefaultLibraryUrl();
        inputParameterResolver = IInputParameterResolver.createResolver(
                libraryEngine.getRepository(),
                this.subjectId,
                this.encounterId,
                this.practitionerId,
                this.parameters,
                this.useServerData,
                this.data);
    }

    public IBaseResource getActivityDefinition() {
        return activityDefinition;
    }

    @Override
    public String getOperationName() {
        return "apply";
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

    protected final String resolveDefaultLibraryUrl() {
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

    @Override
    public IBaseResource getQuestionnaire() {
        return null;
    }
}
