package org.opencds.cqf.fhir.cr.questionnaire.generate;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cql.engine.model.FhirModelResolverCache;
import org.opencds.cqf.fhir.cr.common.ICpgRequest;

public class GenerateRequest implements ICpgRequest {
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;

    // test constructor
    public GenerateRequest(FhirVersionEnum fhirVersion) {
        this.subjectId = null;
        this.parameters = null;
        this.bundle = null;
        this.libraryEngine = null;
        this.modelResolver = FhirModelResolverCache.resolverForVersion(fhirVersion);
        this.fhirVersion = fhirVersion;
        this.defaultLibraryUrl = null;
    }

    public GenerateRequest(
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirVersion =
                libraryEngine.getRepository().fhirContext().getVersion().getVersion();
        this.defaultLibraryUrl = "";
    }

    @Override
    public String getOperationName() {
        return "questionnaire";
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    @Override
    public IBaseBundle getBundle() {
        return bundle;
    }

    @Override
    public IBaseParameters getParameters() {
        return parameters;
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
    public IBaseResource getQuestionnaire() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getQuestionnaire'");
    }

    @Override
    public IBaseOperationOutcome getOperationOutcome() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOperationOutcome'");
    }

    @Override
    public void setOperationOutcome(IBaseOperationOutcome operationOutcome) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setOperationOutcome'");
    }

    @Override
    public IIdType getEncounterId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getEncounterId'");
    }

    @Override
    public IIdType getPractitionerId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPractitionerId'");
    }

    @Override
    public IIdType getOrganizationId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getOrganizationId'");
    }

    @Override
    public IBaseDatatype getUserType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserType'");
    }

    @Override
    public IBaseDatatype getUserLanguage() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserLanguage'");
    }

    @Override
    public IBaseDatatype getUserTaskContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUserTaskContext'");
    }

    @Override
    public IBaseDatatype getSetting() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSetting'");
    }

    @Override
    public IBaseDatatype getSettingContext() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSettingContext'");
    }

    @Override
    public Boolean getUseServerData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getUseServerData'");
    }
}
