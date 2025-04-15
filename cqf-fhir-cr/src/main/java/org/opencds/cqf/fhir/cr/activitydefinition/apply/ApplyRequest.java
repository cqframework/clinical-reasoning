package org.opencds.cqf.fhir.cr.activitydefinition.apply;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;
import org.opencds.cqf.fhir.cr.common.IInputParameterResolver;
import org.opencds.cqf.fhir.utility.adapter.IActivityDefinitionAdapter;
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;

public class ApplyRequest implements ICpgRequest {
    private final IActivityDefinitionAdapter activityDefinitionAdapter;
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
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final Map<String, String> referencedLibraries;
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
        checkNotNull(activityDefinition, "expected non-null value for activityDefinition");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        fhirVersion = activityDefinition.getStructureFhirVersionEnum();
        this.activityDefinitionAdapter = getAdapterFactory().createActivityDefinition(activityDefinition);
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
        referencedLibraries = activityDefinitionAdapter.getReferencedLibraries();
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
        return activityDefinitionAdapter.get();
    }

    @Override
    public String getOperationName() {
        return "apply";
    }

    @Override
    public IBase getContextVariable() {
        return getActivityDefinition();
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
    public Map<String, Object> getRawParameters() {
        // TODO: do this
        return null;
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

    //    @Override
    //    public String getDefaultLibraryUrl() {
    //        return defaultLibraryUrl;
    //    }

    @Override
    public Map<String, String> getReferencedLibraries() {
        return referencedLibraries;
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
    public IBaseResource getQuestionnaire() {
        return null;
    }

    @Override
    public IQuestionnaireAdapter getQuestionnaireAdapter() {
        return null;
    }
}
