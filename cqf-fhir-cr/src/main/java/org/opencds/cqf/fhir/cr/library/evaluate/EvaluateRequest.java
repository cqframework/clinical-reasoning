package org.opencds.cqf.fhir.cr.library.evaluate;

import static com.google.common.base.Preconditions.checkNotNull;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.IOperationRequest;

public class EvaluateRequest implements IOperationRequest {
    private final IBaseResource library;
    private final IIdType subjectId;
    private final Set<String> expression;
    private final IBaseParameters parameters;
    private final Boolean useServerData;
    private final IBaseBundle data;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private IBaseOperationOutcome operationOutcome;

    public EvaluateRequest(
            IBaseResource library,
            IIdType subjectId,
            List<String> expression,
            IBaseParameters parameters,
            Boolean useServerData,
            IBaseBundle data,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.library = library;
        this.subjectId = subjectId;
        this.expression = expression == null ? null : new HashSet<>(expression);
        this.parameters = parameters;
        this.useServerData = useServerData;
        this.data = data;
        this.libraryEngine = libraryEngine;
        this.modelResolver = modelResolver;
        fhirVersion = library.getStructureFhirVersionEnum();
    }

    public IBaseResource getLibrary() {
        return library;
    }

    public Set<String> getExpression() {
        return expression;
    }

    @Override
    public String getOperationName() {
        return "evaluate";
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    public String getSubject() {
        return subjectId == null ? null : subjectId.getValueAsString();
    }

    @Override
    public IBaseBundle getData() {
        return data;
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
        return resolvePathString(library, "url");
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
    public boolean getUseServerData() {
        return useServerData;
    }
}
