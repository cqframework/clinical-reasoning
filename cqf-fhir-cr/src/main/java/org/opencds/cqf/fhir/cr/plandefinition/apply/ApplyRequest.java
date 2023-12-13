package org.opencds.cqf.fhir.cr.plandefinition.apply;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public class ApplyRequest implements IOperationRequest {
    private final IIdType subjectId;
    private final IBaseParameters parameters;
    private final IBaseBundle bundle;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final String defaultLibraryUrl;

    public ApplyRequest(
            IIdType subjectId,
            IBaseParameters parameters,
            IBaseBundle bundle,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver,
            FhirVersionEnum fhirVersion) {
        this.subjectId = subjectId;
        this.parameters = parameters;
        this.bundle = bundle;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        this.fhirVersion = fhirVersion;
        this.defaultLibraryUrl = resolveDefaultLibraryUrl();
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

    // @SuppressWarnings("unchecked")
    protected final String resolveDefaultLibraryUrl() {
        // var libraryExt = getExtensions(questionnaire).stream()
        //                 .filter(e -> e.getUrl().equals(Constants.CQF_LIBRARY))
        //                 .findFirst()
        //                 .orElse(null);
        // return libraryExt == null ? null : ((IPrimitiveType<String>) libraryExt.getValue()).getValue();
        return null;
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
}
