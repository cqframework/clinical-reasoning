package org.opencds.cqf.fhir.cr.cql.evaluate;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opencds.cqf.fhir.utility.BundleHelper.newBundle;
import static org.opencds.cqf.fhir.utility.Parameters.newParameters;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IBaseParameters;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.opencds.cqf.cql.engine.model.ModelResolver;
import org.opencds.cqf.fhir.cql.LibraryEngine;
import org.opencds.cqf.fhir.cr.common.ICqlOperationRequest;
import org.opencds.cqf.fhir.utility.Canonicals;
import org.opencds.cqf.fhir.utility.model.FhirModelResolverCache;

@SuppressWarnings("UnstableApiUsage")
public class CqlEvaluationRequest implements ICqlOperationRequest {
    private final IIdType subjectId;
    private final String expression;
    private final IBaseParameters parameters;
    private final IBaseBundle data;
    private final String content;
    private final LibraryEngine libraryEngine;
    private final ModelResolver modelResolver;
    private final FhirVersionEnum fhirVersion;
    private final Map<String, String> referencedLibraries;
    private IBaseOperationOutcome operationOutcome;

    public CqlEvaluationRequest(
            IIdType subjectId,
            String expression,
            IBaseParameters parameters,
            List<? extends IBaseBackboneElement> library,
            IBaseBundle data,
            List<? extends IBaseBackboneElement> prefetchData,
            String content,
            LibraryEngine libraryEngine,
            ModelResolver modelResolver) {
        checkNotNull(libraryEngine, "expected non-null value for libraryEngine");
        fhirVersion = libraryEngine.getRepository().fhirContext().getVersion().getVersion();
        this.libraryEngine = libraryEngine;
        this.subjectId = subjectId;
        if (expression == null && content == null) {
            throw new IllegalArgumentException(
                    "The $cql operation requires the expression parameter and/or content parameter to exist");
        }
        this.expression = expression;
        this.parameters = parameters == null ? newParameters(getFhirContext()) : parameters;
        if (prefetchData != null && !prefetchData.isEmpty()) {
            if (data == null) {
                data = newBundle(fhirVersion);
            }
            resolvePrefetchData(data, prefetchData);
        }
        this.data = data;
        this.content = content;
        referencedLibraries = resolveIncludedLibraries(library);
        this.modelResolver =
                modelResolver != null ? modelResolver : FhirModelResolverCache.resolverForVersion(fhirVersion);
    }

    public String getExpression() {
        return expression;
    }

    public String getContent() {
        return content;
    }

    @Override
    public IBase getContextVariable() {
        return null;
    }

    @Override
    public IIdType getSubjectId() {
        return subjectId;
    }

    public String getSubject() {
        return getSubjectId() == null ? null : getSubjectId().getValue();
    }

    @Override
    public IBaseParameters getParameters() {
        return parameters;
    }

    @Override
    public IBaseBundle getData() {
        return data;
    }

    @Override
    public LibraryEngine getLibraryEngine() {
        return libraryEngine;
    }

    @Override
    public String getOperationName() {
        return "cql";
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

    @SuppressWarnings("unchecked")
    protected Map<String, String> resolveIncludedLibraries(List<? extends IBaseBackboneElement> includedLibraries) {
        if (includedLibraries != null && !includedLibraries.isEmpty()) {
            Map<String, String> libraries = new HashMap<>();
            includedLibraries.stream()
                    .map(l -> getAdapterFactory().createParametersParameter(l))
                    .forEach(library -> {
                        if (library.hasPart("url")) {
                            var url = ((IPrimitiveType<String>)
                                            library.getPartValues("url").get(0))
                                    .getValueAsString();
                            var name = library.hasPart("name")
                                    ? ((IPrimitiveType<String>) library.getPartValues("name")
                                                    .get(0))
                                            .getValueAsString()
                                    : Canonicals.getIdPart(url);
                            libraries.put(name, url);
                        }
                    });
            return libraries;
        }
        return Collections.emptyMap();
    }
}
