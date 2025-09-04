package org.opencds.cqf.fhir.cr.graphdefintion.apply;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.cr.common.IInputParameterResolver.createResolver;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.repository.IRepository;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
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
import org.opencds.cqf.fhir.utility.adapter.IQuestionnaireAdapter;

public class ApplyRequest implements ICpgRequest {

    private final IIdType subjectId;
    private final IBaseDatatype userType;
    private final IBaseDatatype userLanguage;
    private final IBaseDatatype userTaskContext;
    private final IBaseDatatype setting;
    private final IBaseDatatype settingContext;
    private final IBaseParameters parameters;
    private final ZonedDateTime startDateTime;
    private final ZonedDateTime endDateTime;
    private IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final IInputParameterResolver inputParameterResolver;
    private IBaseOperationOutcome operationOutcome;
    private IBaseResource graphDefinition;

    public ApplyRequest(
            IBaseResource graphDefinition,
            IIdType subjectId,
            IBaseDatatype userType,
            IBaseDatatype userLanguage,
            IBaseDatatype userTaskContext,
            IBaseDatatype setting,
            IBaseDatatype settingContext,
            IBaseParameters parameters,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            ZonedDateTime startDateTime,
            ZonedDateTime endDateTime,
            IInputParameterResolver inputParameterResolver) {

        checkNotNull(graphDefinition, "expected non-null value for graphDefinition");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");

        this.fhirVersion = graphDefinition.getStructureFhirVersionEnum();
        this.graphDefinition = graphDefinition;
        this.subjectId = subjectId;
        this.userType = userType;
        this.userLanguage = userLanguage;
        this.userTaskContext = userTaskContext;
        this.setting = setting;
        this.settingContext = settingContext;
        this.parameters = parameters;

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
                : createResolver(libraryEngine.getRepository(), this.subjectId, null, null, this.parameters, this.data);

        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public ZonedDateTime getStartDateTime() {
        return startDateTime;
    }

    public ZonedDateTime getEndDateTime() {
        return endDateTime;
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    @Override
    public IIdType getPractitionerId() {
        return null;
    }

    @Override
    public IIdType getEncounterId() {
        return null;
    }

    @Override
    public IIdType getOrganizationId() {
        return null;
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
    public Map<String, String> getReferencedLibraries() {
        return Collections.emptyMap();
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
    public IBase getContextVariable() {
        return null;
    }

    @Override
    public IBaseResource getQuestionnaire() {
        return null;
    }

    @Override
    public IQuestionnaireAdapter getQuestionnaireAdapter() {

        return null;
    }

    public IBaseResource getGraphDefinition() {
        return graphDefinition;
    }

    public ApplyRequest setData(IBaseBundle bundle) {
        data = bundle;
        return this;
    }

    @Override
    public String toString() {
        return String.format(
                "ApplyRequest[subjectId=%s, userType=%s, userLanguage=%s, userTaskContext=%s, setting=%s, settingContext=%s, parameters=%s, data=%s]",
                subjectId, userType, userLanguage, userTaskContext, setting, settingContext, parameters, data);
    }

    @Override
    public IRepository getRepository() {
        return libraryEngine.getRepository();
    }
}
