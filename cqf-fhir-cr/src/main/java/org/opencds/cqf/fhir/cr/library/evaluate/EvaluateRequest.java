package org.opencds.cqf.fhir.cr.library.evaluate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ICqlOperationRequest;

public class EvaluateRequest implements ICqlOperationRequest {
    private final IBaseResource library;
    private final IIdType subjectId;
    private final Set<String> expression;
    private final IBaseParameters parameters;
    private final boolean useServerData;
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
            boolean useServerData,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(library, "expected non-null value for library");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        checkNotNull(modelResolver, "expected non-null value for modelResolver");
        this.library = library;
        fhirVersion = library.getStructureFhirVersionEnum();
        this.subjectId = subjectId;
        this.expression = expression == null ? null : new HashSet<>(expression);
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
    public IBase getContext() {
        return getLibrary();
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
