package org.opencds.cqf.fhir.cr.library.evaluate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.opencds.cqf.fhir.utility.adapter.ILibraryAdapter;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

public class EvaluateRequest implements ICqlOperationRequest {
    private final ILibraryAdapter libraryAdapter;
    private final IIdType subjectId;
    private final Set<String> expression;
    private final IBaseParameters parameters;
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
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(library, "expected non-null value for library");
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        fhirVersion = library.getStructureFhirVersionEnum();
        libraryAdapter = getAdapterFactory().createLibrary(library);
        this.subjectId = subjectId;
        this.expression = expression == null ? null : new HashSet<>(expression);
        this.parameters = parameters;
        if (prefetchData != null && !prefetchData.isEmpty()) {
            if (data == null) {
                data = newBundle(fhirVersion);
            }
            resolvePrefetchData(data, prefetchData);
        }
        this.data = data;
        this.libraryEngine = libraryEngine;
        this.modelResolver =
                modelResolver != null ? modelResolver : FhirModelResolverCache.resolverForVersion(fhirVersion);
    }

    public IBaseResource getLibrary() {
        return libraryAdapter.get();
    }

    public ILibraryAdapter getLibraryAdapter() {
        return libraryAdapter;
    }

    public Set<String> getExpression() {
        return expression;
    }

    @Override
    public String getOperationName() {
        return "evaluate";
    }

    @Override
    public IBase getContextVariable() {
        // The is used for FHIRPath evaluation which the $evaluate operation does not support
        return null;
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
    public Map<String, String> getReferencedLibraries() {
        return Map.of(libraryAdapter.getName(), libraryAdapter.getUrl());
    }

    @Override
    public IBaseOperationOutcome getOperationOutcome() {
        return operationOutcome;
    }

    @Override
    public void setOperationOutcome(IBaseOperationOutcome operationOutcome) {
        this.operationOutcome = operationOutcome;
    }
}
